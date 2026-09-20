package com.codethatmakessense.shop.order.adapter.jpa;

import com.codethatmakessense.shop.order.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import com.codethatmakessense.shop.order.domain.Cancellation;
import com.codethatmakessense.shop.order.domain.CustomerEmail;
import com.codethatmakessense.shop.order.domain.Order;
import com.codethatmakessense.shop.order.domain.OrderLine;
import com.codethatmakessense.shop.order.domain.Payment;
import com.codethatmakessense.shop.order.domain.Shipment;
import com.codethatmakessense.shop.order.domain.ShipmentLine;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderRow {

    @Id
    private Long id;

    private String customerEmail;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDate placedOn;

    private long totalCents;

    private LocalDate paidOn;

    private String paymentReference;

    private LocalDate shippedOn;

    private String carrier;

    private String trackingNumber;

    private LocalDate cancelledOn;

    private String cancellationReason;

    private boolean giftWrap;

    private String giftMessage;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineRow> lines = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentRow> shipments = new ArrayList<>();

    static OrderRow newFrom(Order order) {
        OrderRow row = new OrderRow();
        row.id = order.id().value();
        row.customerEmail = order.customer().value();
        row.placedOn = order.placedOn();
        row.giftWrap = order.giftWrap();
        for (OrderLine line : order.lines()) {
            OrderLineRow lineRow = new OrderLineRow();
            lineRow.setOrder(row);
            lineRow.setSku(line.sku().value());
            lineRow.setQuantity(line.quantity().value());
            lineRow.setUnitPriceCents(line.unitPrice().cents());
            row.lines.add(lineRow);
        }
        row.update(order);
        return row;
    }

    void update(Order order) {
        status = order.status();
        totalCents = order.total().cents();
        order.payment().ifPresent(payment -> {
            paidOn = payment.paidOn();
            paymentReference = payment.reference();
        });
        order.cancellation().ifPresent(cancellation -> {
            cancelledOn = cancellation.cancelledOn();
            cancellationReason = cancellation.reason();
        });
        List<Shipment> domainShipments = order.shipments();
        for (int i = shipments.size(); i < domainShipments.size(); i++) {
            shipments.add(ShipmentRow.from(this, domainShipments.get(i)));
        }
        if (shippedOn == null && !domainShipments.isEmpty()) {
            Shipment first = domainShipments.getFirst();
            shippedOn = first.shippedOn();
            carrier = first.carrier();
            trackingNumber = first.trackingNumber();
        }
    }

    Order toDomain() {
        List<OrderLine> domainLines = new ArrayList<>();
        for (OrderLineRow line : lines) {
            domainLines.add(new OrderLine(new Sku(line.getSku()), new Quantity(line.getQuantity()),
                    new Money(line.getUnitPriceCents())));
        }
        Payment payment = null;
        if (paidOn != null) {
            payment = new Payment(paymentReference, paidOn);
        }
        Cancellation cancellation = null;
        if (cancelledOn != null) {
            cancellation = new Cancellation(cancellationReason, cancelledOn);
        }
        List<Shipment> domainShipments = new ArrayList<>();
        for (ShipmentRow shipment : shipments) {
            domainShipments.add(shipment.toDomain());
        }
        return Order.reconstitute(new OrderId(id), new CustomerEmail(customerEmail), domainLines, placedOn,
                giftWrap, status, payment, domainShipments, cancellation);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDate getPlacedOn() {
        return placedOn;
    }

    public void setPlacedOn(LocalDate placedOn) {
        this.placedOn = placedOn;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public void setTotalCents(long totalCents) {
        this.totalCents = totalCents;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public void setPaidOn(LocalDate paidOn) {
        this.paidOn = paidOn;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public LocalDate getShippedOn() {
        return shippedOn;
    }

    public void setShippedOn(LocalDate shippedOn) {
        this.shippedOn = shippedOn;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public LocalDate getCancelledOn() {
        return cancelledOn;
    }

    public void setCancelledOn(LocalDate cancelledOn) {
        this.cancelledOn = cancelledOn;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public boolean isGiftWrap() {
        return giftWrap;
    }

    public void setGiftWrap(boolean giftWrap) {
        this.giftWrap = giftWrap;
    }

    public String getGiftMessage() {
        return giftMessage;
    }

    public void setGiftMessage(String giftMessage) {
        this.giftMessage = giftMessage;
    }

    public List<OrderLineRow> getLines() {
        return lines;
    }

    public void setLines(List<OrderLineRow> lines) {
        this.lines = lines;
    }

    public List<ShipmentRow> getShipments() {
        return shipments;
    }

    public void setShipments(List<ShipmentRow> shipments) {
        this.shipments = shipments;
    }
}
