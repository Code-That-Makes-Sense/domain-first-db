package com.codethatmakessense.shop.returns.adapter.memory;

import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryReturnRequestRepository implements ReturnRequestRepository {

    private final Map<ReturnId, ReturnRequest> requests = new HashMap<>();

    @Override
    public Optional<ReturnRequest> findById(ReturnId id) {
        return Optional.ofNullable(requests.get(id));
    }

    @Override
    public void save(ReturnRequest request) {
        requests.put(request.id(), request);
    }

    @Override
    public Quantity alreadyReturned(OrderId orderId, Sku sku) {
        Quantity total = Quantity.NONE;
        for (ReturnRequest request : requests.values()) {
            boolean sameItem = request.orderId().equals(orderId) && request.sku().equals(sku);
            if (sameItem && request.countsAsReturned()) {
                total = total.plus(request.quantity());
            }
        }
        return total;
    }
}
