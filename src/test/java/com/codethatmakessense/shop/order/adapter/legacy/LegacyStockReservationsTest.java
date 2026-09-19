package com.codethatmakessense.shop.order.adapter.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import com.codethatmakessense.shop.stock.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LegacyStockReservationsTest {

    @Autowired
    StockService stock;

    @Autowired
    LegacyStockReservations reservations;

    @Test
    void translatesThePortOntoTheLegacyService() {
        stock.receive("LAMP-3", 4);
        Sku lamp = new Sku("LAMP-3");

        reservations.reserve(lamp, new Quantity(3));
        assertThat(stock.available("LAMP-3")).isEqualTo(1);

        reservations.release(lamp, new Quantity(1));
        assertThat(stock.available("LAMP-3")).isEqualTo(2);

        reservations.consume(lamp, new Quantity(2));
        assertThat(stock.available("LAMP-3")).isEqualTo(2);
        assertThatThrownBy(() -> reservations.reserve(lamp, new Quantity(3)))
                .isInstanceOf(IllegalStateException.class);
    }
}
