package com.codethatmakessense.shop.order.adapter.jpa;

import com.codethatmakessense.shop.order.adapter.OrderRepositoryContract;
import com.codethatmakessense.shop.order.domain.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaOrderRepository.class)
class JpaOrderRepositoryTest extends OrderRepositoryContract {

    @Autowired
    JpaOrderRepository repository;

    @Override
    protected OrderRepository repository() {
        return repository;
    }
}
