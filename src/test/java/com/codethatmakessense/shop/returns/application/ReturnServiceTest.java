package com.codethatmakessense.shop.returns.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codethatmakessense.shop.returns.adapter.memory.InMemoryReturnRequestRepository;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnNotAllowed;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnStatus;
import com.codethatmakessense.shop.returns.domain.ShippedItem;
import com.codethatmakessense.shop.shared.Sku;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ReturnServiceTest {

    static final OrderId ORDER = new OrderId(42);
    static final Sku BOOK = new Sku("BOOK-1");
    static final LocalDate SHIPPED_ON = LocalDate.of(2026, 9, 1);
    static final Clock TEN_DAYS_LATER = Clock.fixed(
            SHIPPED_ON.plusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    InMemoryReturnRequestRepository requests = new InMemoryReturnRequestRepository();
    StubShippedItems shippedItems = new StubShippedItems()
            .shipped(new ShippedItem(ORDER, BOOK, new Quantity(2), new Money(1_500), SHIPPED_ON));
    ReturnService returns = new ReturnService(requests, shippedItems, TEN_DAYS_LATER);

    @Test
    void requestsAReturnForWhatShipped() {
        ReturnId id = returns.request(ORDER, BOOK, new Quantity(1));

        ReturnRequest stored = requests.findById(id).orElseThrow();
        assertThat(stored.status()).isEqualTo(ReturnStatus.REQUESTED);
        assertThat(stored.refundAmount()).isEqualTo(new Money(1_500));
    }

    @Test
    void refusesWhatNeverShipped() {
        assertThatThrownBy(() -> returns.request(ORDER, new Sku("MUG-7"), new Quantity(1)))
                .isInstanceOf(ReturnNotAllowed.class)
                .hasMessageContaining("Nothing of MUG-7");
    }

    @Test
    void countsEarlierReturnsAgainstTheSecondRequest() {
        returns.request(ORDER, BOOK, new Quantity(1));
        returns.request(ORDER, BOOK, new Quantity(1));

        assertThatThrownBy(() -> returns.request(ORDER, BOOK, new Quantity(1)))
                .isInstanceOf(ReturnNotAllowed.class);
    }

    @Test
    void aRejectedReturnFreesItsQuantityAgain() {
        ReturnId first = returns.request(ORDER, BOOK, new Quantity(2));
        returns.reject(first);

        ReturnId second = returns.request(ORDER, BOOK, new Quantity(2));

        assertThat(requests.findById(second)).isPresent();
    }

    @Test
    void walksARequestToRefund() {
        ReturnId id = returns.request(ORDER, BOOK, new Quantity(1));

        returns.approve(id);
        returns.receive(id);
        returns.refund(id);

        assertThat(requests.findById(id).orElseThrow().status()).isEqualTo(ReturnStatus.REFUNDED);
    }
}
