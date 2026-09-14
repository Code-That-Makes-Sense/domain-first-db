package com.codethatmakessense.shop.returns.domain;

public class ReturnRequest {

    private ReturnStatus status;

    private ReturnRequest(ReturnStatus status) {
        this.status = status;
    }

    public static ReturnRequest request() {
        return new ReturnRequest(ReturnStatus.REQUESTED);
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

    public ReturnStatus status() {
        return status;
    }
}
