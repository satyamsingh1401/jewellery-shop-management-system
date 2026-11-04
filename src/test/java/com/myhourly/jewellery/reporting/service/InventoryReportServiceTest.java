package com.myhourly.jewellery.reporting.service;

import com.myhourly.jewellery.reporting.dto.InventorySummaryResponse;
import com.myhourly.jewellery.reporting.dto.StockLevelResponse;
import com.myhourly.jewellery.reporting.mapper.ReportMapper;
import com.myhourly.jewellery.reporting.model.StockLevel;
import com.myhourly.jewellery.reporting.repository.InventoryReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReportServiceTest {

    @Mock
    private InventoryReportRepository repository;

    private InventoryReportService service;

    private static final List<StockLevel> STOCK = List.of(
            new StockLevel(1L, "SKU-1", "Ring", "RING", 10, 3),
            new StockLevel(2L, "SKU-2", "Chain", "CHAIN", 2, 5),
            new StockLevel(3L, "SKU-3", "Bangle", "BANGLE", 4, 4),
            new StockLevel(4L, "SKU-4", "Earring", "EARRING", 20, 0));

    @BeforeEach
    void setUp() {
        service = new InventoryReportService(repository, new ReportMapper());
    }

    @Test
    void summaryIncludesAllItems() {
        when(repository.findAllStockLevels()).thenReturn(STOCK);

        InventorySummaryResponse response = service.summary(false);

        assertThat(response.totalProducts()).isEqualTo(4);
        assertThat(response.totalUnits()).isEqualTo(36);
        assertThat(response.lowStockCount()).isEqualTo(2);
        assertThat(response.items()).extracting(StockLevelResponse::productId).containsExactly(1L, 2L, 3L, 4L);
        assertThat(response.items()).extracting(StockLevelResponse::lowStock)
                .containsExactly(false, true, true, false);
    }

    @Test
    void lowStockOnlyFiltersItemsButNotTotals() {
        when(repository.findAllStockLevels()).thenReturn(STOCK);

        InventorySummaryResponse response = service.summary(true);

        assertThat(response.totalProducts()).isEqualTo(4);
        assertThat(response.totalUnits()).isEqualTo(36);
        assertThat(response.lowStockCount()).isEqualTo(2);
        assertThat(response.items()).extracting(StockLevelResponse::productId).containsExactly(2L, 3L);
        assertThat(response.items()).allMatch(StockLevelResponse::lowStock);
    }

    @Test
    void emptyInventoryProducesZeroes() {
        when(repository.findAllStockLevels()).thenReturn(List.of());

        InventorySummaryResponse response = service.summary(true);

        assertThat(response.totalProducts()).isZero();
        assertThat(response.totalUnits()).isZero();
        assertThat(response.lowStockCount()).isZero();
        assertThat(response.items()).isEmpty();
    }

    @Test
    void totalUnitsDoesNotOverflowInt() {
        when(repository.findAllStockLevels()).thenReturn(List.of(
                new StockLevel(1L, "A", "A", "C", Integer.MAX_VALUE, 0),
                new StockLevel(2L, "B", "B", "C", Integer.MAX_VALUE, 0)));

        InventorySummaryResponse response = service.summary(false);

        assertThat(response.totalUnits()).isEqualTo(2L * Integer.MAX_VALUE);
    }
}
