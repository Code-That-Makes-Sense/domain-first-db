package com.codethatmakessense.shop.returns.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReturnRequestTest {

    @Test
    void walksFromRequestedToRefunded() {
        ReturnRequest request = requested();

        request.approve();
        request.receive();
        request.refund();

        assertThat(request.status()).isEqualTo(ReturnStatus.REFUNDED);
    }

    @Test
    void refusesToSkipASteps() {
        ReturnRequest request = requested();

        assertThatThrownBy(request::refund).isInstanceOf(ReturnNotAllowed.class);
        assertThatThrownBy(request::receive).isInstanceOf(ReturnNotAllowed.class);
    }

    @Test
    void refusesToApproveTwice() {
        ReturnRequest request = requested();
        request.approve();

        assertThatThrownBy(request::approve).isInstanceOf(ReturnNotAllowed.class);
    }

    private static ReturnRequest requested() {
        return ReturnRequest.request();
    }
}
