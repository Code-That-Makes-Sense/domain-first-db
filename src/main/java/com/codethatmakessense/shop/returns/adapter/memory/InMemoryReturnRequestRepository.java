package com.codethatmakessense.shop.returns.adapter.memory;

import com.codethatmakessense.shop.returns.domain.OrderId;
import com.codethatmakessense.shop.returns.domain.Quantity;
import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.returns.domain.Sku;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryReturnRequestRepository implements ReturnRequestRepository {

    private final List<ReturnRequest> requests = new ArrayList<>();

    @Override
    public Optional<ReturnRequest> findById(ReturnId id) {
        for (ReturnRequest request : requests) {
            if (request.id().equals(id)) {
                return Optional.of(request);
            }
        }
        return Optional.empty();
    }

    @Override
    public void save(ReturnRequest request) {
        requests.add(request);
    }

    @Override
    public Quantity alreadyReturned(OrderId orderId, Sku sku) {
        Quantity total = Quantity.NONE;
        for (ReturnRequest request : requests) {
            boolean sameItem = request.orderId().equals(orderId) && request.sku().equals(sku);
            if (sameItem && request.countsAsReturned()) {
                total = total.plus(request.quantity());
            }
        }
        return total;
    }
}
