package com.codethatmakessense.shop.returns.domain;

public record Quantity(int value) {

    public static final Quantity NONE = new Quantity(0);

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("A quantity cannot be negative");
        }
    }

    public boolean exceeds(Quantity other) {
        return value > other.value;
    }

    public boolean isNone() {
        return value == 0;
    }
}
