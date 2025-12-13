package com.coticbet.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.coticbet.domain.entity.MoneyRequest;
import com.coticbet.domain.enums.RequestStatus;

@Repository
public interface MoneyRequestRepository extends MongoRepository<MoneyRequest, String> {

    List<MoneyRequest> findByUserId(String userId);

    List<MoneyRequest> findByStatus(RequestStatus status);

    List<MoneyRequest> findByUserIdAndStatus(String userId, RequestStatus status);
}
