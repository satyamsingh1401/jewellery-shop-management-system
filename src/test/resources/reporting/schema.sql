DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    order_status VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    subtotal_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NULL,
    tax_amount DECIMAL(12,2) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    net_weight DECIMAL(10,3),
    rate_per_gram_snapshot DECIMAL(12,2),
    making_charge_snapshot DECIMAL(12,2),
    tax_snapshot DECIMAL(12,2),
    line_total DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50),
    name VARCHAR(150) NOT NULL,
    category VARCHAR(80) NOT NULL,
    metal_type VARCHAR(20) NOT NULL,
    purity VARCHAR(20) NOT NULL,
    gross_weight DECIMAL(12,3) NOT NULL,
    stone_weight DECIMAL(12,3) NOT NULL,
    net_weight DECIMAL(12,3) NOT NULL,
    making_charge_type VARCHAR(20) NOT NULL,
    making_charge_value DECIMAL(12,2) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    available_quantity INT NOT NULL,
    reorder_level INT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
