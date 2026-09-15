package com.codethatmakessense.shop.returns.domain;

import java.util.Optional;

public interface ReturnRequestRepository {

    Optional<ReturnRequest> findById(ReturnId id);

    void save(ReturnRequest request);
}
