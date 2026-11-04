package com.myhourly.jewellery.reporting.repository;

import com.myhourly.jewellery.reporting.model.StockLevel;
import com.myhourly.jewellery.reporting.schema.SourceSchema.Inventory;
import com.myhourly.jewellery.reporting.schema.SourceSchema.Products;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcInventoryReportRepository implements InventoryReportRepository {

    private static final String STOCK_LEVELS_SQL = """
            SELECT i.%1$s AS product_id,
                   p.%2$s AS sku,
                   p.%3$s AS name,
                   p.%4$s AS category,
                   i.%5$s AS available_quantity,
                   i.%6$s AS reorder_level
            FROM %7$s i
            LEFT JOIN %8$s p ON p.%9$s = i.%1$s
            ORDER BY i.%1$s
            """.formatted(
            Inventory.PRODUCT_ID, Products.SKU, Products.NAME, Products.CATEGORY,
            Inventory.AVAILABLE_QUANTITY, Inventory.REORDER_LEVEL,
            Inventory.TABLE, Products.TABLE, Products.ID);

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcInventoryReportRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<StockLevel> findAllStockLevels() {
        return jdbc.query(STOCK_LEVELS_SQL, this::mapStockLevel);
    }

    private StockLevel mapStockLevel(ResultSet rs, int rowNum) throws SQLException {
        return new StockLevel(
                rs.getLong("product_id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getInt("available_quantity"),
                rs.getInt("reorder_level")
        );
    }
}
