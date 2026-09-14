package com.codethatmakessense.shop.returns.domain;

public class ReturnNotAllowed extends RuntimeException {

    public ReturnNotAllowed(String message) {
        super(message);
    }
}
