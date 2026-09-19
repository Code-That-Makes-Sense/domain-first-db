package com.codethatmakessense.shop.returns.domain;

import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.util.Optional;

public interface ReturnRequestRepository {

    Optional<ReturnRequest> findById(ReturnId id);

    void save(ReturnRequest request);

    Quantity alreadyReturned(OrderId orderId, Sku sku);
}
