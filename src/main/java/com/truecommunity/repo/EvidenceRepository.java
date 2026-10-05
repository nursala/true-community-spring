package com.truecommunity.repo;

import com.truecommunity.model.Evidence;
import com.truecommunity.model.Statement;
import com.truecommunity.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for evidence links attached to votes.
 */
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    Optional<Evidence> findByVote(Vote vote);

    List<Evidence> findByVote_Statement(Statement statement);
}
