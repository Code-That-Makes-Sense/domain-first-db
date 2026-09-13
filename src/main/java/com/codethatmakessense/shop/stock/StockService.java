package com.codethatmakessense.shop.stock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final StockRepository items;

    public StockService(StockRepository items) {
        this.items = items;
    }

    @Transactional
    public void receive(String sku, int quantity) {
        StockItem item = items.findById(sku).orElseGet(() -> {
            StockItem created = new StockItem();
            created.setSku(sku);
            return created;
        });
        item.setOnHand(item.getOnHand() + quantity);
        items.save(item);
    }

    @Transactional
    public void reserve(String sku, int quantity) {
        StockItem item = items.findById(sku)
                .orElseThrow(() -> new IllegalStateException("Unknown SKU: " + sku));
        int available = item.getOnHand() - item.getReserved();
        if (available < quantity) {
            throw new IllegalStateException("Not enough stock for " + sku);
        }
        item.setReserved(item.getReserved() + quantity);
    }

    @Transactional
    public void release(String sku, int quantity) {
        StockItem item = items.findById(sku).orElseThrow();
        item.setReserved(item.getReserved() - quantity);
    }

    @Transactional
    public void consume(String sku, int quantity) {
        StockItem item = items.findById(sku).orElseThrow();
        item.setReserved(item.getReserved() - quantity);
        item.setOnHand(item.getOnHand() - quantity);
    }

    public int available(String sku) {
        StockItem item = items.findById(sku).orElseThrow();
        return item.getOnHand() - item.getReserved();
    }
}
