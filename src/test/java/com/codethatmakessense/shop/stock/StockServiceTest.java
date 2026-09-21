package com.codethatmakessense.shop.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codethatmakessense.shop.BootsSpring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@BootsSpring
@SpringBootTest
@Transactional
class StockServiceTest {

    @Autowired
    StockService stock;

    @Test
    void reservationsReduceWhatIsAvailable() {
        stock.receive("LAMP-3", 4);

        stock.reserve("LAMP-3", 3);

        assertThat(stock.available("LAMP-3")).isEqualTo(1);
    }

    @Test
    void refusesToReserveMoreThanAvailable() {
        stock.receive("LAMP-3", 4);

        assertThatThrownBy(() -> stock.reserve("LAMP-3", 5))
                .isInstanceOf(IllegalStateException.class);
    }
}
