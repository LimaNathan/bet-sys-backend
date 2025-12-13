package com.coticbet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.coticbet.domain.entity.Event;
import com.coticbet.domain.enums.EventStatus;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {

    Optional<Event> findByExternalId(String externalId);

    List<Event> findByStatus(EventStatus status);

    List<Event> findByStatusIn(List<EventStatus> statuses);

    List<Event> findByCategory(com.coticbet.domain.enums.EventCategory category);
}
