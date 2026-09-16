package com.codethatmakessense.shop.returns.adapter.memory;

import com.codethatmakessense.shop.returns.adapter.ReturnRequestRepositoryContract;
import com.codethatmakessense.shop.returns.domain.OrderId;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;

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
