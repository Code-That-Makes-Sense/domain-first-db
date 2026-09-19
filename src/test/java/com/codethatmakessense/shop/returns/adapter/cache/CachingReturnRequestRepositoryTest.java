package com.codethatmakessense.shop.returns.adapter.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.codethatmakessense.shop.returns.adapter.ReturnRequestRepositoryContract;
import com.codethatmakessense.shop.returns.adapter.memory.InMemoryReturnRequestRepository;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.returns.domain.ReturnStatus;
import com.codethatmakessense.shop.shared.Sku;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CachingReturnRequestRepositoryTest extends ReturnRequestRepositoryContract {

    private final CountingRepository counting = new CountingRepository(new InMemoryReturnRequestRepository());
    private final CachingReturnRequestRepository repository = new CachingReturnRequestRepository(counting);

    @Override
    protected ReturnRequestRepository repository() {
        return repository;
    }

    @Override
    protected OrderId anOrder() {
        return new OrderId(42);
    }

    @Test
    void asksTheInnerRepositoryOnlyUntilItHasSeenTheRequest() {
        ReturnId id = ReturnId.next();
        repository.findById(id);
        repository.findById(id);
        assertThat(counting.reads.get()).isEqualTo(2);

        repository.save(ReturnRequest.reconstitute(id, anOrder(), new Sku("BOOK-1"), new Quantity(1),
                LocalDate.of(2026, 9, 1), new Money(1_500), ReturnStatus.REQUESTED));
        repository.findById(id);
        repository.findById(id);

        assertThat(counting.reads.get()).isEqualTo(2);
    }

    static class CountingRepository implements ReturnRequestRepository {

        final AtomicInteger reads = new AtomicInteger();
        private final ReturnRequestRepository inner;

        CountingRepository(ReturnRequestRepository inner) {
            this.inner = inner;
        }

        @Override
        public Optional<ReturnRequest> findById(ReturnId id) {
            reads.incrementAndGet();
            return inner.findById(id);
        }

        @Override
        public void save(ReturnRequest request) {
            inner.save(request);
        }

        @Override
        public Quantity alreadyReturned(OrderId orderId, Sku sku) {
            return inner.alreadyReturned(orderId, sku);
        }
    }
}
