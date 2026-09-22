package com.codethatmakessense.shop.returns.adapter.jpa;

import com.codethatmakessense.shop.BootsSpring;
import com.codethatmakessense.shop.returns.adapter.ReturnRequestRepositoryContract;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.shared.OrderId;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@BootsSpring
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaReturnRequestRepository.class)
class JpaReturnRequestRepositoryTest extends ReturnRequestRepositoryContract {

    @Autowired
    JpaReturnRequestRepository repository;

    @Autowired
    JdbcClient jdbc;

    @Override
    protected ReturnRequestRepository repository() {
        return repository;
    }

    @Override
    protected OrderId anOrder() {
        long id = jdbc.sql("SELECT NEXT VALUE FOR orders_id_seq").query(Long.class).single();
        jdbc.sql("INSERT INTO orders (id, customer_email, status, placed_on, total_cents) VALUES (?, ?, ?, ?, ?)")
                .params(id, "viktor@example.com", "SHIPPED", LocalDate.of(2026, 9, 1), 7_500)
                .update();
        return new OrderId(id);
    }
}
