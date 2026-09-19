package com.codethatmakessense.shop.shared;

public record Sku(String value) {

    public Sku {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A SKU cannot be blank");
        }
    }
}
