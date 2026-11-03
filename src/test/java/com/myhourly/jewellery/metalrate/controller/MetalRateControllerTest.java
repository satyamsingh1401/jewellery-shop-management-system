package com.myhourly.jewellery.metalrate.controller;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.DuplicateResourceException;
import com.myhourly.jewellery.common.exception.GlobalExceptionHandler;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.metalrate.dto.MetalRatePageResponse;
import com.myhourly.jewellery.metalrate.dto.MetalRateRequest;
import com.myhourly.jewellery.metalrate.dto.MetalRateResponse;
import com.myhourly.jewellery.metalrate.dto.MetalValueRequest;
import com.myhourly.jewellery.metalrate.dto.MetalValueResponse;
import com.myhourly.jewellery.metalrate.enums.MetalType;
import com.myhourly.jewellery.metalrate.enums.Purity;
import com.myhourly.jewellery.metalrate.exception.MetalRateRequestExceptionHandler;
import com.myhourly.jewellery.metalrate.service.MetalRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MetalRateControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 0, 0);

    private MetalRateService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(MetalRateService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MetalRateController(service))
                .setControllerAdvice(new MetalRateRequestExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(service.createRate(any(MetalRateRequest.class))).thenReturn(sampleRate());

        mockMvc.perform(post("/api/metal-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metalType":"GOLD","purity":"K22","ratePerGram":7000.00,"remarks":"Morning rate"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/metal-rates/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.metalType").value("GOLD"))
                .andExpect(jsonPath("$.purity").value("K22"))
                .andExpect(jsonPath("$.ratePerGram").value(7000.00))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.createdBy").value("admin@shop.com"));
    }

    @Test
    void createRejectsInvalidBodyWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/metal-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metalType":"GOLD","ratePerGram":-5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/metal-rates"))
                .andExpect(jsonPath("$.fieldErrors.purity").value("Purity is required"))
                .andExpect(jsonPath("$.fieldErrors.ratePerGram").value("Rate per gram must be greater than zero"));
        verify(service, never()).createRate(any());
    }

    @Test
    void createRejectsUnknownEnumValue() throws Exception {
        mockMvc.perform(post("/api/metal-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metalType":"BRONZE","purity":"K22","ratePerGram":10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMapsBusinessRuleViolationTo400() throws Exception {
        when(service.createRate(any(MetalRateRequest.class)))
                .thenThrow(new BusinessValidationException("Purity K22 is not valid for metal SILVER"));

        mockMvc.perform(post("/api/metal-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metalType":"SILVER","purity":"K22","ratePerGram":95}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Purity K22 is not valid for metal SILVER"));
    }

    @Test
    void createMapsDuplicateTo409() throws Exception {
        when(service.createRate(any(MetalRateRequest.class)))
                .thenThrow(new DuplicateResourceException("duplicate"));

        mockMvc.perform(post("/api/metal-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metalType":"GOLD","purity":"K22","ratePerGram":7000}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void currentReturnsRate() throws Exception {
        when(service.getCurrentRate(MetalType.GOLD, Purity.K22)).thenReturn(sampleRate());

        mockMvc.perform(get("/api/metal-rates/current")
                        .param("metalType", "GOLD")
                        .param("purity", "K22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratePerGram").value(7000.00))
                .andExpect(jsonPath("$.effectiveFrom").value("2026-11-04T10:00:00"));
    }

    @Test
    void currentReturns404WhenNoRateExists() throws Exception {
        when(service.getCurrentRate(MetalType.GOLD, Purity.K18))
                .thenThrow(new ResourceNotFoundException("No rate found"));

        mockMvc.perform(get("/api/metal-rates/current")
                        .param("metalType", "GOLD")
                        .param("purity", "K18"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No rate found"));
    }

    @Test
    void currentRejectsMissingParameter() throws Exception {
        mockMvc.perform(get("/api/metal-rates/current").param("metalType", "GOLD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.purity").value("Parameter is required"));
    }

    @Test
    void currentRejectsInvalidEnumParameter() throws Exception {
        mockMvc.perform(get("/api/metal-rates/current")
                        .param("metalType", "GOLD")
                        .param("purity", "K99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.purity").value("Invalid value"));
    }

    @Test
    void allCurrentReturnsList() throws Exception {
        when(service.getAllCurrentRates()).thenReturn(List.of(sampleRate()));

        mockMvc.perform(get("/api/metal-rates/current/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].purity").value("K22"));
    }

    @Test
    void applicableParsesIsoTimestamp() throws Exception {
        when(service.getApplicableRate(MetalType.GOLD, Purity.K22, NOW)).thenReturn(sampleRate());

        mockMvc.perform(get("/api/metal-rates/applicable")
                        .param("metalType", "GOLD")
                        .param("purity", "K22")
                        .param("at", "2026-11-04T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void historyPassesFiltersAndDefaults() throws Exception {
        when(service.getHistory(MetalType.GOLD, null, null, null, 0, 20))
                .thenReturn(new MetalRatePageResponse(List.of(sampleRate()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/metal-rates/history").param("metalType", "GOLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getByIdReturnsRate() throws Exception {
        when(service.getById(1L)).thenReturn(sampleRate());

        mockMvc.perform(get("/api/metal-rates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void metalValueReturnsCalculation() throws Exception {
        when(service.calculateMetalValue(any(MetalValueRequest.class))).thenReturn(new MetalValueResponse(
                MetalType.GOLD, Purity.K22, new BigDecimal("10.000"), 1L,
                new BigDecimal("7000.00"), "INR", NOW, new BigDecimal("70000.00")));

        mockMvc.perform(post("/api/metal-rates/metal-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metalType":"GOLD","purity":"K22","netWeight":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metalValue").value(70000.00))
                .andExpect(jsonPath("$.rateId").value(1));
    }

    @Test
    void metalValueRejectsZeroWeight() throws Exception {
        mockMvc.perform(post("/api/metal-rates/metal-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metalType":"GOLD","purity":"K22","netWeight":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.netWeight").value("Net weight must be greater than zero"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/metal-rates/3"))
                .andExpect(status().isNoContent());
        verify(service).deleteScheduledRate(3L);
    }

    @Test
    void deleteActiveRateReturns400() throws Exception {
        doThrow(new BusinessValidationException("Only rates scheduled for the future can be deleted"))
                .when(service).deleteScheduledRate(4L);

        mockMvc.perform(delete("/api/metal-rates/4"))
                .andExpect(status().isBadRequest());
    }

    private static MetalRateResponse sampleRate() {
        return new MetalRateResponse(
                1L, MetalType.GOLD, Purity.K22, new BigDecimal("7000.00"), "INR",
                NOW, "Morning rate", "admin@shop.com", NOW);
    }
}
