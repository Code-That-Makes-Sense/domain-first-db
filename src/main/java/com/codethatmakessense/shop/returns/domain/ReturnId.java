package com.codethatmakessense.shop.returns.domain;

import java.util.UUID;

public record ReturnId(UUID value) {

    public static ReturnId next() {
        return new ReturnId(UUID.randomUUID());
    }
}
