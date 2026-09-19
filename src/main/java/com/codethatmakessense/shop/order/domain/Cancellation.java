package com.codethatmakessense.shop.order.domain;

import java.time.LocalDate;

public record Cancellation(String reason, LocalDate cancelledOn) {
}
