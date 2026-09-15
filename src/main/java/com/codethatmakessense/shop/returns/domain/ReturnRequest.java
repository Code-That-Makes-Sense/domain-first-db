package com.codethatmakessense.shop.returns.domain;

public class ReturnRequest {

    private final Quantity quantity;
    private final Money refund;
    private ReturnStatus status;

    private ReturnRequest(Quantity quantity, Money refund, ReturnStatus status) {
        this.quantity = quantity;
        this.refund = refund;
        this.status = status;
    }

    public static ReturnRequest request(ShippedItem shipped, Quantity quantity) {
        if (quantity.isNone()) {
            throw new ReturnNotAllowed("Nothing to return");
        }
        if (quantity.exceeds(shipped.quantity())) {
            throw new ReturnNotAllowed("Only " + shipped.quantity().value() + " of " + shipped.sku().value() + " shipped");
        }
        Money refund = shipped.unitPrice().times(quantity);
        return new ReturnRequest(quantity, refund, ReturnStatus.REQUESTED);
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

    public Quantity quantity() {
        return quantity;
    }

    public Money refundAmount() {
        return refund;
    }

    public ReturnStatus status() {
        return status;
    }
}
