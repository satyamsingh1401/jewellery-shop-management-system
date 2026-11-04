package com.myhourly.jewellery.reporting.schema;

public final class SourceSchema {

    private SourceSchema() {
    }

    public static final class Orders {
        public static final String TABLE = "orders";
        public static final String ID = "id";
        public static final String ORDER_STATUS = "order_status";
        public static final String PAYMENT_STATUS = "payment_status";
        public static final String SUBTOTAL = "subtotal_amount";
        public static final String DISCOUNT = "discount_amount";
        public static final String TAX = "tax_amount";
        public static final String TOTAL = "total_amount";
        public static final String CREATED_AT = "created_at";

        private Orders() {
        }
    }

    public static final class OrderItems {
        public static final String TABLE = "order_items";
        public static final String ORDER_ID = "order_id";
        public static final String PRODUCT_ID = "product_id";
        public static final String QUANTITY = "quantity";
        public static final String NET_WEIGHT = "net_weight";
        public static final String LINE_TOTAL = "line_total";

        private OrderItems() {
        }
    }

    public static final class Products {
        public static final String TABLE = "products";
        public static final String ID = "id";
        public static final String SKU = "sku";
        public static final String NAME = "name";
        public static final String CATEGORY = "category";

        private Products() {
        }
    }

    public static final class Inventory {
        public static final String TABLE = "inventory";
        public static final String PRODUCT_ID = "product_id";
        public static final String AVAILABLE_QUANTITY = "available_quantity";
        public static final String REORDER_LEVEL = "reorder_level";

        private Inventory() {
        }
    }
}
