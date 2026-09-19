package com.codethatmakessense.shop.returns.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReturnRequestTest {

    static final LocalDate SHIPPED_ON = LocalDate.of(2026, 9, 1);
    static final ShippedItem TWO_BOOKS = new ShippedItem(
            new OrderId(42), new Sku("BOOK-1"), new Quantity(2), new Money(1_500), SHIPPED_ON);

    @Test
    void refundsThePriceOfWhatComesBack() {
        ReturnRequest request = ReturnRequest.request(
                ReturnId.next(), TWO_BOOKS, Quantity.NONE, new Quantity(2), SHIPPED_ON.plusDays(3));

        assertThat(request.status()).isEqualTo(ReturnStatus.REQUESTED);
        assertThat(request.refundAmount()).isEqualTo(new Money(3_000));
    }

    @Test
    void refusesMoreThanWhatShippedMinusWhatCameBackAlready() {
        assertThatThrownBy(() -> ReturnRequest.request(
                ReturnId.next(), TWO_BOOKS, new Quantity(1), new Quantity(2), SHIPPED_ON.plusDays(3)))
                .isInstanceOf(ReturnNotAllowed.class)
                .hasMessageContaining("Only 1");
    }

    @Test
    void refusesAReturnAfterThirtyDays() {
        assertThatThrownBy(() -> ReturnRequest.request(
                ReturnId.next(), TWO_BOOKS, Quantity.NONE, new Quantity(1), SHIPPED_ON.plusDays(31)))
                .isInstanceOf(ReturnNotAllowed.class)
                .hasMessageContaining("window");
    }

    @Test
    void acceptsAReturnOnTheLastDayOfTheWindow() {
        ReturnRequest request = ReturnRequest.request(
                ReturnId.next(), TWO_BOOKS, Quantity.NONE, new Quantity(1), SHIPPED_ON.plusDays(30));

        assertThat(request.status()).isEqualTo(ReturnStatus.REQUESTED);
    }

    @Test
    void refusesToReturnNothing() {
        assertThatThrownBy(() -> ReturnRequest.request(
                ReturnId.next(), TWO_BOOKS, Quantity.NONE, Quantity.NONE, SHIPPED_ON))
                .isInstanceOf(ReturnNotAllowed.class);
    }

    @Test
    void walksFromRequestedToRefunded() {
        ReturnRequest request = requested();

        request.approve();
        request.receive();
        request.refund();

        assertThat(request.status()).isEqualTo(ReturnStatus.REFUNDED);
    }

    @Test
    void aRejectedRequestStopsCountingAsReturned() {
        ReturnRequest request = requested();

        request.reject();

        assertThat(request.status()).isEqualTo(ReturnStatus.REJECTED);
        assertThat(request.countsAsReturned()).isFalse();
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
        return ReturnRequest.request(ReturnId.next(), TWO_BOOKS, Quantity.NONE, new Quantity(1), SHIPPED_ON.plusDays(1));
    }
}
