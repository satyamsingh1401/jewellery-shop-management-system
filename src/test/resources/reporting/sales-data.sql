INSERT INTO orders (id, order_number, customer_id, order_status, payment_status, subtotal_amount, discount_amount, tax_amount, total_amount, created_by, created_at, updated_at) VALUES
(1, 'ORD-001', 1, 'COMPLETED', 'PAID', 1000.00, 50.00, 30.00, 980.00, 'staff', TIMESTAMP '2026-03-10 00:00:00', NULL),
(2, 'ORD-002', 2, 'CONFIRMED', 'PAID', 2000.00, NULL, 60.00, 2060.00, 'staff', TIMESTAMP '2026-03-10 23:59:59', NULL),
(3, 'ORD-003', 3, 'COMPLETED', 'REFUNDED', 500.00, 0.00, 15.00, 515.00, 'staff', TIMESTAMP '2026-03-10 12:00:00', NULL),
(4, 'ORD-004', 4, 'CANCELLED', 'PAID', 700.00, 0.00, 21.00, 721.00, 'staff', TIMESTAMP '2026-03-10 10:00:00', NULL),
(5, 'ORD-005', 5, 'PENDING', 'PENDING', 900.00, 0.00, 27.00, 927.00, 'staff', TIMESTAMP '2026-03-10 11:00:00', NULL),
(6, 'ORD-006', 6, 'COMPLETED', 'PAID', 300.00, 0.00, 9.00, 309.00, 'staff', TIMESTAMP '2026-03-11 00:00:00', NULL),
(7, 'ORD-007', 7, 'COMPLETED', 'PAID', 400.00, 10.00, 12.00, 402.00, 'staff', TIMESTAMP '2026-03-09 23:59:59', NULL),
(8, 'ORD-008', 8, 'CANCELLED', 'REFUNDED', 800.00, 0.00, 24.00, 824.00, 'staff', TIMESTAMP '2026-03-10 15:00:00', NULL),
(9, 'ORD-009', 9, 'CANCELLED', 'PAID', 600.00, 0.00, 18.00, 618.00, 'staff', TIMESTAMP '2026-03-12 09:00:00', NULL),
(10, 'ORD-010', 10, 'PENDING', 'PENDING', 100.00, 0.00, 3.00, 103.00, 'staff', TIMESTAMP '2026-03-13 09:00:00', NULL);

INSERT INTO order_items (id, order_id, product_id, quantity, net_weight, rate_per_gram_snapshot, making_charge_snapshot, tax_snapshot, line_total) VALUES
(1, 1, 101, 2, 3.500, 80.00, 10.00, 5.00, 600.00),
(2, 1, 102, 1, 10.000, 40.00, 50.00, 5.00, 500.00),
(3, 2, 101, 1, 3.500, 100.00, 40.00, 10.00, 400.00),
(4, 2, 999, 1, 2.000, 450.00, 80.00, 20.00, 1000.00),
(5, 2, 103, 1, 5.000, 80.00, 80.00, 20.00, 500.00),
(6, 3, 104, 1, 2.000, 4000.00, 999.00, 999.00, 9999.00),
(7, 3, 101, 1, 3.500, 2000.00, 999.00, 999.00, 9999.00),
(8, 4, 104, 1, 2.000, 4000.00, 888.00, 888.00, 8888.00),
(9, 5, 104, 1, 2.000, 3000.00, 777.00, 777.00, 7777.00),
(10, 6, 104, 1, 2.000, 3000.00, 666.00, 666.00, 6666.00),
(11, 7, 104, 1, 2.000, 2500.00, 555.00, 555.00, 5555.00),
(12, 8, 104, 1, 2.000, 2000.00, 444.00, 444.00, 4444.00);
