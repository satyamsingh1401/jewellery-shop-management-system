package com.myhourly.jewellery.reporting.repository;

import com.myhourly.jewellery.reporting.config.ReportingProperties;
import com.myhourly.jewellery.reporting.model.DailySalesFigures;
import com.myhourly.jewellery.reporting.model.ProductSalesFigures;
import com.myhourly.jewellery.reporting.model.ReportPeriod;
import com.myhourly.jewellery.reporting.model.SalesFigures;
import com.myhourly.jewellery.reporting.schema.SourceSchema.OrderItems;
import com.myhourly.jewellery.reporting.schema.SourceSchema.Orders;
import com.myhourly.jewellery.reporting.schema.SourceSchema.Products;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class JdbcSalesReportRepository implements SalesReportRepository {

    private static final String IS_SALE = "o." + Orders.ORDER_STATUS + " IN (:saleStatuses)";
    private static final String IS_REFUNDED = "o." + Orders.PAYMENT_STATUS + " = :refundedStatus";
    private static final String IS_CANCELLED = "o." + Orders.ORDER_STATUS + " = :cancelledStatus";
    private static final String IN_PERIOD =
            "o." + Orders.CREATED_AT + " >= :fromTime AND o." + Orders.CREATED_AT + " < :toTime";

    private static final String DAILY_SALES_SQL = """
            SELECT CAST(o.%1$s AS DATE) AS sale_date,
                   SUM(CASE WHEN %2$s THEN 1 ELSE 0 END) AS order_count,
                   SUM(CASE WHEN %2$s THEN o.%3$s END) AS subtotal_amount,
                   SUM(CASE WHEN %2$s THEN o.%4$s END) AS discount_amount,
                   SUM(CASE WHEN %2$s THEN o.%5$s END) AS tax_amount,
                   SUM(CASE WHEN %2$s THEN o.%6$s END) AS total_amount,
                   SUM(CASE WHEN %2$s AND %7$s THEN 1 ELSE 0 END) AS refunded_count,
                   SUM(CASE WHEN %2$s AND %7$s THEN o.%6$s END) AS refunded_amount,
                   SUM(CASE WHEN %8$s THEN 1 ELSE 0 END) AS cancelled_count
            FROM %9$s o
            WHERE %10$s
            GROUP BY CAST(o.%1$s AS DATE)
            ORDER BY sale_date
            """.formatted(
            Orders.CREATED_AT, IS_SALE, Orders.SUBTOTAL, Orders.DISCOUNT, Orders.TAX, Orders.TOTAL,
            IS_REFUNDED, IS_CANCELLED, Orders.TABLE, IN_PERIOD);

    private static final String TOP_PRODUCTS_SQL = """
            SELECT oi.%1$s AS product_id,
                   p.%2$s AS sku,
                   p.%3$s AS name,
                   p.%4$s AS category,
                   SUM(oi.%5$s) AS quantity_sold,
                   SUM(oi.%6$s * oi.%5$s) AS weight_sold,
                   SUM(oi.%7$s) AS revenue
            FROM %8$s oi
            JOIN %9$s o ON o.%10$s = oi.%11$s
            LEFT JOIN %12$s p ON p.%13$s = oi.%1$s
            WHERE %14$s
              AND %15$s
              AND (o.%16$s IS NULL OR o.%16$s <> :refundedStatus)
            GROUP BY oi.%1$s, p.%2$s, p.%3$s, p.%4$s
            ORDER BY revenue DESC, product_id
            LIMIT :limit
            """.formatted(
            OrderItems.PRODUCT_ID, Products.SKU, Products.NAME, Products.CATEGORY,
            OrderItems.QUANTITY, OrderItems.NET_WEIGHT, OrderItems.LINE_TOTAL,
            OrderItems.TABLE, Orders.TABLE, Orders.ID, OrderItems.ORDER_ID,
            Products.TABLE, Products.ID, IN_PERIOD, IS_SALE, Orders.PAYMENT_STATUS);

    private final NamedParameterJdbcTemplate jdbc;
    private final ReportingProperties properties;

    public JdbcSalesReportRepository(NamedParameterJdbcTemplate jdbc, ReportingProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    @Override
    public List<DailySalesFigures> findDailySalesFigures(ReportPeriod period) {
        return jdbc.query(DAILY_SALES_SQL, baseParameters(period), this::mapDailyFigures);
    }

    @Override
    public List<ProductSalesFigures> findTopProductSales(ReportPeriod period, int limit) {
        MapSqlParameterSource parameters = baseParameters(period).addValue("limit", limit);
        return jdbc.query(TOP_PRODUCTS_SQL, parameters, this::mapProductFigures);
    }

    private MapSqlParameterSource baseParameters(ReportPeriod period) {
        return new MapSqlParameterSource()
                .addValue("fromTime", Timestamp.valueOf(period.startInclusive()))
                .addValue("toTime", Timestamp.valueOf(period.endExclusive()))
                .addValue("saleStatuses", properties.saleOrderStatuses())
                .addValue("refundedStatus", properties.refundedPaymentStatus())
                .addValue("cancelledStatus", properties.cancelledOrderStatus());
    }

    private DailySalesFigures mapDailyFigures(ResultSet rs, int rowNum) throws SQLException {
        SalesFigures figures = new SalesFigures(
                rs.getLong("order_count"),
                rs.getBigDecimal("subtotal_amount"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("tax_amount"),
                rs.getBigDecimal("total_amount"),
                rs.getLong("refunded_count"),
                rs.getBigDecimal("refunded_amount"),
                rs.getLong("cancelled_count")
        );
        return new DailySalesFigures(rs.getDate("sale_date").toLocalDate(), figures);
    }

    private ProductSalesFigures mapProductFigures(ResultSet rs, int rowNum) throws SQLException {
        return new ProductSalesFigures(
                rs.getLong("product_id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getLong("quantity_sold"),
                rs.getBigDecimal("weight_sold"),
                rs.getBigDecimal("revenue")
        );
    }
}
