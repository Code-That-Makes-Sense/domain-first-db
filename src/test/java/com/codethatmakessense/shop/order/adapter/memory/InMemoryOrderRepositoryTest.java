package com.codethatmakessense.shop.order.adapter.memory;

import com.codethatmakessense.shop.order.adapter.OrderRepositoryContract;
import com.codethatmakessense.shop.order.domain.OrderRepository;

class InMemoryOrderRepositoryTest extends OrderRepositoryContract {

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();

    @Override
    protected OrderRepository repository() {
        return repository;
    }
}
