package com.codethatmakessense.shop.returns.adapter.legacy;

import com.codethatmakessense.shop.order.Order;
import com.codethatmakessense.shop.order.OrderLine;
import com.codethatmakessense.shop.order.OrderRepository;
import com.codethatmakessense.shop.order.Shipment;
import com.codethatmakessense.shop.order.ShipmentLine;
import com.codethatmakessense.shop.returns.domain.ShippedItem;
import com.codethatmakessense.shop.returns.domain.ShippedItems;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LegacyShippedItems implements ShippedItems {

    private final OrderRepository orders;

    public LegacyShippedItems(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShippedItem> shippedItem(OrderId orderId, Sku sku) {
        Optional<Order> found = orders.findById(orderId.value());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Order order = found.get();
        Quantity shipped = Quantity.NONE;
        LocalDate firstShippedOn = null;
        for (Shipment shipment : order.getShipments()) {
            for (ShipmentLine line : shipment.getLines()) {
                if (line.getSku().equals(sku.value())) {
                    shipped = shipped.plus(new Quantity(line.getQuantity()));
                    if (firstShippedOn == null || shipment.getShippedOn().isBefore(firstShippedOn)) {
                        firstShippedOn = shipment.getShippedOn();
                    }
                }
            }
        }
        if (shipped.isNone()) {
            return Optional.empty();
        }
        return Optional.of(new ShippedItem(orderId, sku, shipped, unitPrice(order, sku), firstShippedOn));
    }

    private Money unitPrice(Order order, Sku sku) {
        for (OrderLine line : order.getLines()) {
            if (line.getSku().equals(sku.value())) {
                return new Money(line.getUnitPriceCents());
            }
        }
        throw new IllegalStateException("Shipped a SKU the order never had: " + sku.value());
    }
}
