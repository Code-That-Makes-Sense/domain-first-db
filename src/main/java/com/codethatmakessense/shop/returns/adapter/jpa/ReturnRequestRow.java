package com.codethatmakessense.shop.returns.adapter.jpa;

import com.codethatmakessense.shop.returns.domain.Money;
import com.codethatmakessense.shop.returns.domain.OrderId;
import com.codethatmakessense.shop.returns.domain.Quantity;
import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnStatus;
import com.codethatmakessense.shop.returns.domain.Sku;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
class ReturnRequestRow {

    @Id
    private UUID id;

    private long orderId;

    private String sku;

    private int quantity;

    private LocalDate requestedOn;

    private long refundCents;

    private String status;

    protected ReturnRequestRow() {
    }

    static ReturnRequestRow from(ReturnRequest request) {
        ReturnRequestRow row = new ReturnRequestRow();
        row.id = request.id().value();
        row.orderId = request.orderId().value();
        row.sku = request.sku().value();
        row.quantity = request.quantity().value();
        row.requestedOn = request.requestedOn();
        row.refundCents = request.refundAmount().cents();
        row.status = request.status().name();
        return row;
    }

    ReturnRequest toDomain() {
        return ReturnRequest.reconstitute(
                new ReturnId(id), new OrderId(orderId), new Sku(sku), new Quantity(quantity),
                requestedOn, new Money(refundCents), ReturnStatus.valueOf(status));
    }

    int quantity() {
        return quantity;
    }

    ReturnStatus status() {
        return ReturnStatus.valueOf(status);
    }
}
