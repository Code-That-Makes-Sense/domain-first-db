package com.codethatmakessense.shop.returns.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.codethatmakessense.shop.returns.domain.Money;
import com.codethatmakessense.shop.returns.domain.OrderId;
import com.codethatmakessense.shop.returns.domain.Quantity;
import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.returns.domain.ReturnStatus;
import com.codethatmakessense.shop.returns.domain.ShippedItem;
import com.codethatmakessense.shop.returns.domain.Sku;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public abstract class ReturnRequestRepositoryContract {

    static final LocalDate SHIPPED_ON = LocalDate.of(2026, 9, 1);
    static final Sku BOOK = new Sku("BOOK-1");

    protected abstract ReturnRequestRepository repository();

    protected abstract OrderId anOrder();

    @Test
    void findsWhatItSaved() {
        ReturnRequest request = requested(anOrder(), 1);

        repository().save(request);

        ReturnRequest found = repository().findById(request.id()).orElseThrow();
        assertThat(found.id()).isEqualTo(request.id());
        assertThat(found.quantity()).isEqualTo(new Quantity(1));
        assertThat(found.refundAmount()).isEqualTo(new Money(1_500));
        assertThat(found.status()).isEqualTo(ReturnStatus.REQUESTED);
    }

    @Test
    void keepsStateChanges() {
        ReturnRequest request = requested(anOrder(), 1);
        repository().save(request);

        request.approve();
        repository().save(request);

        assertThat(repository().findById(request.id()).orElseThrow().status()).isEqualTo(ReturnStatus.APPROVED);
    }

    @Test
    void sumsWhatAlreadyCameBackExceptRejectedRequests() {
        OrderId order = anOrder();
        repository().save(requested(order, 1));
        repository().save(requested(order, 1));
        ReturnRequest rejected = requested(order, 1);
        rejected.reject();
        repository().save(rejected);

        assertThat(repository().alreadyReturned(order, BOOK)).isEqualTo(new Quantity(2));
        assertThat(repository().alreadyReturned(order, new Sku("MUG-7"))).isEqualTo(Quantity.NONE);
    }

    @Test
    void countsARequestOnceHoweverOftenItIsSaved() {
        OrderId order = anOrder();
        ReturnRequest request = requested(order, 2);
        repository().save(request);
        request.approve();
        repository().save(request);

        assertThat(repository().alreadyReturned(order, BOOK)).isEqualTo(new Quantity(2));
    }

    @Test
    void findsNothingForAnUnknownId() {
        assertThat(repository().findById(ReturnId.next())).isEmpty();
    }

    private static ReturnRequest requested(OrderId order, int quantity) {
        ShippedItem shipped = new ShippedItem(order, BOOK, new Quantity(5), new Money(1_500), SHIPPED_ON);
        return ReturnRequest.request(ReturnId.next(), shipped, Quantity.NONE, new Quantity(quantity), SHIPPED_ON.plusDays(1));
    }
}
