package com.codethatmakessense.shop.returns.application;

import com.codethatmakessense.shop.returns.domain.OrderId;
import com.codethatmakessense.shop.returns.domain.Quantity;
import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnNotAllowed;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.returns.domain.ShippedItem;
import com.codethatmakessense.shop.returns.domain.ShippedItems;
import com.codethatmakessense.shop.returns.domain.Sku;
import java.time.Clock;
import java.time.LocalDate;

public class ReturnService {

    private final ReturnRequestRepository requests;
    private final ShippedItems shippedItems;
    private final Clock clock;

    public ReturnService(ReturnRequestRepository requests, ShippedItems shippedItems, Clock clock) {
        this.requests = requests;
        this.shippedItems = shippedItems;
        this.clock = clock;
    }

    public ReturnId request(OrderId orderId, Sku sku, Quantity quantity) {
        ShippedItem shipped = shippedItems.shippedItem(orderId, sku)
                .orElseThrow(() -> new ReturnNotAllowed(
                        "Nothing of " + sku.value() + " shipped for order " + orderId.value()));
        ReturnRequest request = ReturnRequest.request(
                ReturnId.next(), shipped, quantity, LocalDate.now(clock));
        requests.save(request);
        return request.id();
    }

    public void approve(ReturnId id) {
        ReturnRequest request = load(id);
        request.approve();
        requests.save(request);
    }

    public void reject(ReturnId id) {
        ReturnRequest request = load(id);
        request.reject();
        requests.save(request);
    }

    public void receive(ReturnId id) {
        ReturnRequest request = load(id);
        request.receive();
        requests.save(request);
    }

    public void refund(ReturnId id) {
        ReturnRequest request = load(id);
        request.refund();
        requests.save(request);
    }

    private ReturnRequest load(ReturnId id) {
        return requests.findById(id).orElseThrow();
    }
}
