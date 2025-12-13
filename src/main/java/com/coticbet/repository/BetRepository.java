package com.coticbet.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.coticbet.domain.entity.Bet;
import com.coticbet.domain.enums.BetStatus;

@Repository
public interface BetRepository extends MongoRepository<Bet, String> {

    List<Bet> findByUserId(String userId);

    List<Bet> findByEventId(String eventId);

    List<Bet> findByEventIdAndStatus(String eventId, BetStatus status);

    List<Bet> findByUserIdAndStatus(String userId, BetStatus status);
}
