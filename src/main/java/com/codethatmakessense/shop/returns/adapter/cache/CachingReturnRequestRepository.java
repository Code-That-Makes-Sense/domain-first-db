package com.codethatmakessense.shop.returns.adapter.cache;

import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CachingReturnRequestRepository implements ReturnRequestRepository {

    private final ReturnRequestRepository inner;
    private final Map<ReturnId, ReturnRequest> cache = new ConcurrentHashMap<>();

    public CachingReturnRequestRepository(ReturnRequestRepository inner) {
        this.inner = inner;
    }

    @Override
    public Optional<ReturnRequest> findById(ReturnId id) {
        ReturnRequest cached = cache.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<ReturnRequest> loaded = inner.findById(id);
        loaded.ifPresent(request -> cache.put(id, request));
        return loaded;
    }

    @Override
    public void save(ReturnRequest request) {
        inner.save(request);
        cache.put(request.id(), request);
    }

    @Override
    public Quantity alreadyReturned(OrderId orderId, Sku sku) {
        return inner.alreadyReturned(orderId, sku);
    }
}
