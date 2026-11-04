package com.myhourly.jewellery.reporting.model;

import java.time.LocalDate;

public record DailySalesFigures(LocalDate date, SalesFigures figures) {
}
