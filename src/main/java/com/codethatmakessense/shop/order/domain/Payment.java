package com.codethatmakessense.shop.order.domain;

import java.time.LocalDate;

public record Payment(String reference, LocalDate paidOn) {
}
