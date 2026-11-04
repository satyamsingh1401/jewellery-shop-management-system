package com.myhourly.jewellery.reporting.controller;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.reporting.dto.InventorySummaryResponse;
import com.myhourly.jewellery.reporting.dto.StockLevelResponse;
import com.myhourly.jewellery.reporting.service.InventoryReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryReportController.class)
@WithMockUser
class InventoryReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryReportService inventoryReportService;

    private static final StockLevelResponse LOW = new StockLevelResponse(7L, "NECK-007", "Pearl Necklace",
            "NECKLACE", 1, 3, true);
    private static final StockLevelResponse HEALTHY = new StockLevelResponse(8L, "RING-008", "Silver Ring",
            "RING", 20, 5, false);

    @Test
    void inventoryDefaultsLowStockOnlyToFalse() throws Exception {
        when(inventoryReportService.summary(false))
                .thenReturn(new InventorySummaryResponse(2, 21, 1, List.of(LOW, HEALTHY)));

        mockMvc.perform(get("/api/reports/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(2))
                .andExpect(jsonPath("$.totalUnits").value(21))
                .andExpect(jsonPath("$.lowStockCount").value(1))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].sku").value("NECK-007"))
                .andExpect(jsonPath("$.items[0].lowStock").value(true))
                .andExpect(jsonPath("$.items[1].availableQuantity").value(20))
                .andExpect(jsonPath("$.items[1].lowStock").value(false));

        verify(inventoryReportService).summary(false);
    }

    @Test
    void inventoryPassesLowStockOnlyFlag() throws Exception {
        when(inventoryReportService.summary(true))
                .thenReturn(new InventorySummaryResponse(2, 21, 1, List.of(LOW)));

        mockMvc.perform(get("/api/reports/inventory").param("lowStockOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(7));

        verify(inventoryReportService).summary(true);
    }

    @Test
    void inventoryWithInvalidFlagReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/inventory").param("lowStockOnly", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Parameter 'lowStockOnly' must be true or false"))
                .andExpect(jsonPath("$.path").value("/api/reports/inventory"))
                .andExpect(jsonPath("$.fieldErrors.lowStockOnly").exists());

        verifyNoInteractions(inventoryReportService);
    }

    @Test
    void inventoryBusinessValidationFailureReturnsBadRequest() throws Exception {
        when(inventoryReportService.summary(anyBoolean()))
                .thenThrow(new BusinessValidationException("Inventory report unavailable"));

        mockMvc.perform(get("/api/reports/inventory"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Inventory report unavailable"))
                .andExpect(jsonPath("$.path").value("/api/reports/inventory"));
    }
}
