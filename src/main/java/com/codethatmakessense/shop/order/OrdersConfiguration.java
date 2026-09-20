package com.codethatmakessense.shop.order;

import com.codethatmakessense.shop.order.application.OrderService;
import com.codethatmakessense.shop.order.domain.OrderRepository;
import com.codethatmakessense.shop.order.domain.StockReservations;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrdersConfiguration {

    @Bean
    public OrderService orderService(OrderRepository orders, StockReservations stock, Clock clock) {
        return new OrderService(orders, stock, clock);
    }
}
