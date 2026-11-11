package com.myhourly.jewellery.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class ProbeController {
        @GetMapping("/probe/{id}/availability")
        public int availability(@PathVariable Long id, @RequestParam int quantity) {
            return quantity;
        }

        @GetMapping("/probe/insufficient")
        public int insufficient() {
            throw new InsufficientStockException("Insufficient stock for product 1 (requested 100)");
        }

        @GetMapping("/probe/invalid")
        public int invalid() {
            throw new BusinessValidationException("quantityChange must not be zero");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingQuantityReturnsFullErrorFormat() throws Exception {
        mockMvc.perform(get("/probe/1/availability"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/probe/1/availability"))
                .andExpect(jsonPath("$.fieldErrors.quantity").value("quantity is required"));
    }

    @Test
    void nonNumericQuantityReturnsFullErrorFormat() throws Exception {
        mockMvc.perform(get("/probe/1/availability").param("quantity", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/probe/1/availability"))
                .andExpect(jsonPath("$.fieldErrors.quantity").value("quantity must be a valid int"));
    }

    @Test
    void insufficientStockReturns409() throws Exception {
        mockMvc.perform(get("/probe/insufficient"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product 1 (requested 100)"))
                .andExpect(jsonPath("$.path").value("/probe/insufficient"));
    }

    @Test
    void otherBusinessValidationStillReturns400() throws Exception {
        mockMvc.perform(get("/probe/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("quantityChange must not be zero"));
    }

    @Test
    void nonNumericPathVariableReturnsFullErrorFormat() throws Exception {
        mockMvc.perform(get("/probe/abc/availability").param("quantity", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.id").value("id must be a valid Long"));
    }
}
