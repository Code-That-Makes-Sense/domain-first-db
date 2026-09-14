package com.codethatmakessense.shop.report;

import java.time.LocalDate;

public record DailySales(LocalDate day, long orderCount, long totalCents) {
}
