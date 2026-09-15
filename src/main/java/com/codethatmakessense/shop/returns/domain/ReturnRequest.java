package com.codethatmakessense.shop.returns.domain;

import java.time.LocalDate;
import java.time.Period;

public class ReturnRequest {

    public static final Period RETURN_WINDOW = Period.ofDays(30);

    private final ReturnId id;
    private final Quantity quantity;
    private final LocalDate requestedOn;
    private final LocalDate shippedOn;
    private final Money refund;
    private ReturnStatus status;

    private ReturnRequest(ReturnId id, Quantity quantity, LocalDate requestedOn, LocalDate shippedOn, Money refund,
            ReturnStatus status) {
        this.id = id;
        this.quantity = quantity;
        this.requestedOn = requestedOn;
        this.shippedOn = shippedOn;
        this.refund = refund;
        this.status = status;
    }

    public static ReturnRequest request(ReturnId id, ShippedItem shipped, Quantity quantity, LocalDate requestedOn) {
        if (quantity.isNone()) {
            throw new ReturnNotAllowed("Nothing to return");
        }
        if (requestedOn.isAfter(shipped.shippedOn().plus(RETURN_WINDOW))) {
            throw new ReturnNotAllowed("The return window closed on " + shipped.shippedOn().plus(RETURN_WINDOW));
        }
        if (quantity.exceeds(shipped.quantity())) {
            throw new ReturnNotAllowed("Only " + shipped.quantity().value() + " of " + shipped.sku().value() + " shipped");
        }
        Money refund = shipped.unitPrice().times(quantity);
        return new ReturnRequest(id, quantity, requestedOn, shipped.shippedOn(), refund, ReturnStatus.REQUESTED);
    }

    public void approve() {
        transition(ReturnStatus.REQUESTED, ReturnStatus.APPROVED);
    }

    public void reject() {
        transition(ReturnStatus.REQUESTED, ReturnStatus.REJECTED);
    }

    public void receive() {
        transition(ReturnStatus.APPROVED, ReturnStatus.RECEIVED);
    }

    public void refund() {
        transition(ReturnStatus.RECEIVED, ReturnStatus.REFUNDED);
    }

    private void transition(ReturnStatus from, ReturnStatus to) {
        if (status != from) {
            throw new ReturnNotAllowed("A " + status + " return cannot become " + to);
        }
        status = to;
    }

    public ReturnId id() {
        return id;
    }

    public Quantity quantity() {
        return quantity;
    }

    public LocalDate requestedOn() {
        return requestedOn;
    }

    public LocalDate shippedOn() {
        return shippedOn;
    }

    public Money refundAmount() {
        return refund;
    }

    public ReturnStatus status() {
        return status;
    }
}
