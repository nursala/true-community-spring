package com.truecommunity.repo;

import com.truecommunity.model.Speaker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for speakers referenced by statements.
 */
public interface SpeakerRepository extends JpaRepository<Speaker, Long> {

    List<Speaker> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    Optional<Speaker> findByNameIgnoreCase(String name);
}
