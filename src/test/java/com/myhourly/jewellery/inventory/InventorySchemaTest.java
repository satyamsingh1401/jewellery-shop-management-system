package com.myhourly.jewellery.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:schema-${random.uuid};MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class InventorySchemaTest {

    @MockitoBean
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    JdbcTemplate jdbc;

    private List<String> columns(String table) {
        return jdbc.queryForList(
                "select column_name from information_schema.columns where table_name = ?", String.class, table);
    }

    @Test
    void configuredSchemaStrategyCreatesEveryInventoryTable() {
        assertThat(columns("inventory")).contains("id", "product_id", "available_quantity", "reorder_level",
                "created_at", "updated_at");
        assertThat(columns("stock_movement")).contains("id", "product_id", "movement_type", "quantity_change",
                "balance_after", "reference_type", "reference_id", "reason", "created_by", "created_at");
        assertThat(columns("processed_event")).contains("event_id", "event_type", "processed_at");
        assertThat(columns("cancelled_order")).contains("order_id", "cancelled_at");
    }

    @Test
    void inventoryKeepsTheColumnsTheReportingModuleReads() {
        assertThat(columns("inventory")).contains("product_id", "available_quantity", "reorder_level");
    }

    @Test
    void productIdIsUniqueInInventory() {
        jdbc.update("insert into inventory (product_id, available_quantity, reorder_level, created_at, updated_at) values (777, 1, 0, current_timestamp, current_timestamp)");
        assertThatThrownBy(() -> jdbc.update(
                "insert into inventory (product_id, available_quantity, reorder_level, created_at, updated_at) values (777, 2, 0, current_timestamp, current_timestamp)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void orderReferenceIsUniquePerProductAndMovementType() {
        String sql = "insert into stock_movement (product_id, movement_type, quantity_change, balance_after, "
                + "reference_type, reference_id, created_at) values (5, 'SALE', -1, 0, 'ORDER', 'ORD-1', current_timestamp)";
        jdbc.update(sql);
        assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
