package com.codethatmakessense.shop.order.adapter.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRowRepository extends JpaRepository<OrderRow, Long> {

    @Query(value = "SELECT NEXT VALUE FOR orders_id_seq", nativeQuery = true)
    long nextId();
}
