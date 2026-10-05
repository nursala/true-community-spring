package com.truecommunity.repo;

import com.truecommunity.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for the controlled topic list used by votes and editor decisions.
 */
public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByName(String name);

    List<Topic> findAllByIdIn(Collection<Long> ids);
}
