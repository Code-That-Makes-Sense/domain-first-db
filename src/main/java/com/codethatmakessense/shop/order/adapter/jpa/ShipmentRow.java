package com.codethatmakessense.shop.order.adapter.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import com.codethatmakessense.shop.order.domain.Shipment;
import com.codethatmakessense.shop.order.domain.ShipmentLine;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments")
public class ShipmentRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private OrderRow order;

    private LocalDate shippedOn;

    private String carrier;

    private String trackingNumber;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentLineRow> lines = new ArrayList<>();

    static ShipmentRow from(OrderRow order, Shipment shipment) {
        ShipmentRow row = new ShipmentRow();
        row.order = order;
        row.shippedOn = shipment.shippedOn();
        row.carrier = shipment.carrier();
        row.trackingNumber = shipment.trackingNumber();
        for (ShipmentLine line : shipment.lines()) {
            ShipmentLineRow lineRow = new ShipmentLineRow();
            lineRow.setShipment(row);
            lineRow.setSku(line.sku().value());
            lineRow.setQuantity(line.quantity().value());
            row.lines.add(lineRow);
        }
        return row;
    }

    Shipment toDomain() {
        List<ShipmentLine> domainLines = new ArrayList<>();
        for (ShipmentLineRow line : lines) {
            domainLines.add(new ShipmentLine(new Sku(line.getSku()), new Quantity(line.getQuantity())));
        }
        return new Shipment(shippedOn, carrier, trackingNumber, domainLines);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderRow getOrder() {
        return order;
    }

    public void setOrder(OrderRow order) {
        this.order = order;
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

    public List<ShipmentLineRow> getLines() {
        return lines;
    }

    public void setLines(List<ShipmentLineRow> lines) {
        this.lines = lines;
    }
}
