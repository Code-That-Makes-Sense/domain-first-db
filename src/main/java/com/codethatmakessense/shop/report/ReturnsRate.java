package com.codethatmakessense.shop.report;

public record ReturnsRate(String sku, long shipped, long returned) {

    public double rate() {
        if (shipped == 0) {
            return 0;
        }
        return (double) returned / shipped;
    }
}
