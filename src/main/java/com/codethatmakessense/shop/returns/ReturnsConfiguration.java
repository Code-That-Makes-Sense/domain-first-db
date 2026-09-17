package com.codethatmakessense.shop.returns;

import com.codethatmakessense.shop.returns.adapter.cache.CachingReturnRequestRepository;
import com.codethatmakessense.shop.returns.adapter.jpa.JpaReturnRequestRepository;
import com.codethatmakessense.shop.returns.application.ReturnService;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.returns.domain.ShippedItems;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ReturnsConfiguration {

    @Bean
    @Primary
    public ReturnRequestRepository returnRequests(JpaReturnRequestRepository jpa) {
        return new CachingReturnRequestRepository(jpa);
    }

    @Bean
    public ReturnService returnService(ReturnRequestRepository requests, ShippedItems shippedItems, Clock clock) {
        return new ReturnService(requests, shippedItems, clock);
    }
}
