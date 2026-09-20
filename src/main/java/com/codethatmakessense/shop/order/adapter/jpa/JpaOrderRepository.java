package com.codethatmakessense.shop.order.adapter.jpa;

import com.codethatmakessense.shop.order.domain.Order;
import com.codethatmakessense.shop.order.domain.OrderRepository;
import com.codethatmakessense.shop.shared.OrderId;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaOrderRepository implements OrderRepository {

    private final OrderRowRepository rows;

    public JpaOrderRepository(OrderRowRepository rows) {
        this.rows = rows;
    }

    @Override
    public OrderId nextId() {
        return new OrderId(rows.nextId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId id) {
        return rows.findById(id.value()).map(OrderRow::toDomain);
    }

    @Override
    @Transactional
    public void save(Order order) {
        Optional<OrderRow> existing = rows.findById(order.id().value());
        if (existing.isPresent()) {
            existing.get().update(order);
            return;
        }
        rows.save(OrderRow.newFrom(order));
    }
}
