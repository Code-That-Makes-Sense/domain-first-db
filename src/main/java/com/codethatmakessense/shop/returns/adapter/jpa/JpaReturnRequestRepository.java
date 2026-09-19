package com.codethatmakessense.shop.returns.adapter.jpa;

import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.returns.domain.ReturnStatus;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaReturnRequestRepository implements ReturnRequestRepository {

    private final ReturnRequestRowRepository rows;

    public JpaReturnRequestRepository(ReturnRequestRowRepository rows) {
        this.rows = rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReturnRequest> findById(ReturnId id) {
        return rows.findById(id.value()).map(ReturnRequestRow::toDomain);
    }

    @Override
    @Transactional
    public void save(ReturnRequest request) {
        rows.save(ReturnRequestRow.from(request));
    }

    @Override
    @Transactional(readOnly = true)
    public Quantity alreadyReturned(OrderId orderId, Sku sku) {
        Quantity total = Quantity.NONE;
        for (ReturnRequestRow row : rows.findByOrderIdAndSku(orderId.value(), sku.value())) {
            if (row.status() != ReturnStatus.REJECTED) {
                total = total.plus(new Quantity(row.quantity()));
            }
        }
        return total;
    }
}
