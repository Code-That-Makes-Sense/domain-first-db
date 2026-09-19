package com.codethatmakessense.shop.order.domain;

import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.time.LocalDate;
import java.util.List;

public record Shipment(LocalDate shippedOn, String carrier, String trackingNumber, List<ShipmentLine> lines) {

    public Shipment {
        lines = List.copyOf(lines);
    }

    public Quantity quantityOf(Sku sku) {
        Quantity total = Quantity.NONE;
        for (ShipmentLine line : lines) {
            if (line.sku().equals(sku)) {
                total = total.plus(line.quantity());
            }
        }
        return total;
    }
}
