CREATE TABLE invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_number VARCHAR(40) NOT NULL,
    customer_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    subtotal DECIMAL(14,2) NOT NULL,
    discount_amount DECIMAL(14,2) NOT NULL,
    taxable_amount DECIMAL(14,2) NOT NULL,
    tax_amount DECIMAL(14,2) NOT NULL,
    grand_total DECIMAL(14,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_invoice_number UNIQUE (invoice_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoice_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    product_id BIGINT NULL,
    net_weight DECIMAL(12,3) NOT NULL,
    rate_per_gram DECIMAL(12,2) NOT NULL,
    metal_value DECIMAL(14,2) NOT NULL,
    making_charge DECIMAL(14,2) NOT NULL,
    line_total DECIMAL(14,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;