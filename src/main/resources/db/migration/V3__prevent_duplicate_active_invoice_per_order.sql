ALTER TABLE invoices
    ADD COLUMN active_order_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN status <> 'CANCELLED' THEN order_id ELSE NULL END) STORED;

ALTER TABLE invoices
    ADD CONSTRAINT uk_invoices_active_order UNIQUE (active_order_id);