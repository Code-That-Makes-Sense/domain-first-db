package com.codethatmakessense.shop.order.domain;

public record CustomerEmail(String value) {

    public CustomerEmail {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A customer email cannot be blank");
        }
    }
}
