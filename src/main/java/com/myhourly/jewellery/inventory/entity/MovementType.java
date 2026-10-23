package com.myhourly.jewellery.inventory.entity;

/** Why stock changed. Every change to stock is recorded as one of these. */
public enum MovementType {
    STOCK_IN,    // new stock received into the shop
    SALE,        // stock removed because an order was confirmed
    RETURN,      // stock put back because an order was cancelled
    ADJUSTMENT   // manual correction by an admin (positive or negative)
}
