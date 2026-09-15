package com.codethatmakessense.shop.returns.domain;

public record Sku(String value) {

    public Sku {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A SKU cannot be blank");
        }
    }
}
