# Module 9 – Sales Reports

Read-only reporting over data owned by other modules. This module never writes to another module's tables and imports none of their Java classes.

## Endpoints

| Method | Path | Params | Returns |
|---|---|---|---|
| GET | `/api/reports/daily-sales` | `date` (YYYY-MM-DD, required) | `DailySalesReportResponse` |
| GET | `/api/reports/sales` | `from`, `to` (required) | `SalesRangeReportResponse`: totals plus one row per day (missing days zero-filled) |
| GET | `/api/reports/product-sales` | `from`, `to` (required), `limit` (optional, default 10, max 100) | `ProductSalesReportResponse`: products ranked by revenue |
| GET | `/api/reports/inventory` | `lowStockOnly` (optional, default `false`) | `InventorySummaryResponse` |

Validation errors return `400` with the shared `ApiError` body. Dates must not be in the future, `from <= to`, and a range spans at most 366 days.

Security: none yet. Once Module 10 lands, the spec's permissions table says reports are ADMIN-only.

## Business rules

| Rule | Definition | Where to change it |
|---|---|---|
| Sale | `orders.order_status` in `reporting.sale-order-statuses` (default `CONFIRMED,COMPLETED`). PENDING is ignored. | `application.properties` |
| Cancelled | `order_status = reporting.cancelled-order-status` (default `CANCELLED`). Counted, never added to amounts. | `application.properties` |
| Refund | A sale whose `payment_status = reporting.refunded-payment-status` (default `REFUNDED`). `netAmount = totalAmount − refundedAmount`. Excluded from product sales. | `application.properties` |
| Day | `orders.created_at` in `[date 00:00, date+1 00:00)`. `created_at` is a server-local `LocalDateTime`, so the server must run in the same zone as `reporting.zone`. | `ReportPeriod` |
| Weight sold | `SUM(order_items.net_weight × quantity)`. Assumes `net_weight` is per unit. | `JdbcSalesReportRepository` |
| Low stock | `available_quantity <= reorder_level`, same as the Inventory module | `StockLevel.isLowStock()` |
| Rounding | Money 2 dp, weight 3 dp, HALF_UP | `ReportMapper` |
| Limits | `reporting.max-range-days` (366), `reporting.default-top-limit` (10), `reporting.max-top-limit` (100) | `application.properties` |
| "Today" | `reporting.zone` (default `Asia/Kolkata`) | `application.properties` |

All the `reporting.*` keys are optional. Their defaults live in `ReportingProperties`.

## Source table contract

Every table and column name this module reads is declared once, in `reporting/schema/SourceSchema.java`. If a teammate renames a column, change it there and nowhere else.

| Owner | Table | Columns read |
|---|---|---|
| Order (Module 5) | `orders` | `id`, `order_status`, `payment_status`, `subtotal_amount`, `discount_amount`, `tax_amount`, `total_amount`, `created_at` |
| Order (Module 5) | `order_items` | `order_id`, `product_id`, `quantity`, `net_weight`, `line_total` |
| Product (Module 1) | `products` | `id`, `sku`, `name`, `category` |
| Inventory (Module 3) | `inventory` | `product_id`, `available_quantity`, `reorder_level` |

## Package layout

```
reporting/
  config/       ReportingProperties (tunable rules), ReportingClock, ReportingConfiguration
  schema/       SourceSchema: the only place that knows other modules' table/column names
  model/        internal figures (SalesFigures, ReportPeriod, StockLevel, ...), independent of the API
  repository/   interfaces + JDBC implementations
  service/      one service per report + ReportRequestValidator
  mapper/       model -> response DTO (rounding lives here)
  dto/          API response records
  controller/   thin HTTP layer
  exception/    400 handling for bad or missing query params, scoped to this module
```

## Making changes safely

- **Column/table renamed by another module**: edit `SourceSchema` only.
- **Different sale/refund statuses**: set `reporting.*` properties. No code change.
- **New field in a response**: add it to `model` → repository mapping → `ReportMapper` → DTO. The controllers don't change.
- **New report**: add a repository method, a new service class and a controller method. Existing reports are untouched.
- **Switching data source** (e.g. to a Kafka-fed summary table or the other modules' JPA repositories): write a new implementation of `SalesReportRepository` / `InventoryReportRepository`. The services, DTOs and controllers stay as they are.
- Repository SQL is covered by H2 (MySQL mode) tests in `src/test/java/.../reporting/repository`. Run them after any SQL change:
  `mvnw test -Dtest="com.myhourly.jewellery.reporting.**"`

## Open questions for the team

1. Order module: is `order_items.net_weight` per unit or per line?
2. Order module: should a sale be dated by a `confirmed_at` timestamp instead of `created_at`? No such column exists yet.
3. No module publishes `sales-events / SaleCompleted` yet. A Kafka consumer can be added later as another repository implementation.
