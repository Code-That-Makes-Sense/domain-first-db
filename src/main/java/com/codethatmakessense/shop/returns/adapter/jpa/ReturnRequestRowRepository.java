package com.codethatmakessense.shop.returns.adapter.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReturnRequestRowRepository extends JpaRepository<ReturnRequestRow, UUID> {

    List<ReturnRequestRow> findByOrderIdAndSku(long orderId, String sku);
}
