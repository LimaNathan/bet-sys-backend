package com.coticbet.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.coticbet.domain.entity.Bet;
import com.coticbet.domain.enums.BetStatus;

@Repository
public interface BetRepository extends MongoRepository<Bet, String> {

    List<Bet> findByUserId(String userId);

    List<Bet> findByUserId(String userId, Sort sort);

    List<Bet> findByEventId(String eventId);

    List<Bet> findByEventIdAndStatus(String eventId, BetStatus status);

    List<Bet> findByUserIdAndStatus(String userId, BetStatus status);

    /**
     * Find bets that have a leg with the given eventId and bet status.
     * Used for settling multiple bets when an event is settled.
     */
    @Query("{ 'legs.eventId': ?0, 'status': ?1 }")
    List<Bet> findByLegsEventIdAndStatus(String eventId, BetStatus status);
}
