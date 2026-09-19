package com.codethatmakessense.shop.returns.adapter.memory;

import com.codethatmakessense.shop.returns.adapter.ReturnRequestRepositoryContract;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.shared.OrderId;

class InMemoryReturnRequestRepositoryTest extends ReturnRequestRepositoryContract {

    private final InMemoryReturnRequestRepository repository = new InMemoryReturnRequestRepository();

    @Override
    protected ReturnRequestRepository repository() {
        return repository;
    }

    @Override
    protected OrderId anOrder() {
        return new OrderId(42);
    }
}
