package com.codethatmakessense.shop.order.adapter.memory;

import com.codethatmakessense.shop.order.domain.Order;
import com.codethatmakessense.shop.order.domain.OrderRepository;
import com.codethatmakessense.shop.shared.OrderId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> orders = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public OrderId nextId() {
        return new OrderId(nextId.getAndIncrement());
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public void save(Order order) {
        orders.put(order.id(), order);
    }
}
