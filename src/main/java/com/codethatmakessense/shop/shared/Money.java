package com.codethatmakessense.shop.shared;

public record Money(long cents) {

    public Money times(Quantity quantity) {
        return new Money(cents * quantity.value());
    }
}
