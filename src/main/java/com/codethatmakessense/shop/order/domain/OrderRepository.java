package com.codethatmakessense.shop.order.domain;

import com.codethatmakessense.shop.shared.OrderId;
import java.util.Optional;

public interface OrderRepository {

    OrderId nextId();

    Optional<Order> findById(OrderId id);

    void save(Order order);
}
