package com.myhourly.jewellery.reporting.repository;

import com.myhourly.jewellery.reporting.model.StockLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("reporting-test")
@Import(JdbcInventoryReportRepository.class)
@Sql({"/reporting/schema.sql", "/reporting/products-data.sql", "/reporting/inventory-data.sql"})
class JdbcInventoryReportRepositoryTest {

    @Autowired
    private JdbcInventoryReportRepository repository;

    @Test
    void returnsEveryInventoryRowOrderedByProductId() {
        List<StockLevel> rows = repository.findAllStockLevels();

        assertThat(rows).extracting(StockLevel::productId).containsExactly(101L, 103L, 999L);
    }

    @Test
    void joinsProductDetailsAndFlagsLowStock() {
        List<StockLevel> rows = repository.findAllStockLevels();

        StockLevel ring = rows.get(0);
        assertThat(ring.sku()).isEqualTo("R-101");
        assertThat(ring.name()).isEqualTo("Gold Ring");
        assertThat(ring.category()).isEqualTo("RING");
        assertThat(ring.availableQuantity()).isEqualTo(1);
        assertThat(ring.reorderLevel()).isEqualTo(3);
        assertThat(ring.isLowStock()).isTrue();

        StockLevel bangle = rows.get(1);
        assertThat(bangle.availableQuantity()).isEqualTo(5);
        assertThat(bangle.reorderLevel()).isEqualTo(2);
        assertThat(bangle.isLowStock()).isFalse();
    }

    @Test
    void inventoryRowWithoutProductKeepsNullDetails() {
        StockLevel orphan = repository.findAllStockLevels().get(2);

        assertThat(orphan.productId()).isEqualTo(999L);
        assertThat(orphan.sku()).isNull();
        assertThat(orphan.name()).isNull();
        assertThat(orphan.category()).isNull();
        assertThat(orphan.availableQuantity()).isZero();
    }

    @Test
    @Sql({"/reporting/schema.sql", "/reporting/products-data.sql"})
    void emptyInventoryReturnsNoRows() {
        assertThat(repository.findAllStockLevels()).isEmpty();
    }
}
