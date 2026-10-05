package com.truecommunity.repo;

import com.truecommunity.model.TrustScoreLog;
import com.truecommunity.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for score-change audit records keyed by vote.
 */
public interface TrustScoreLogRepository extends JpaRepository<TrustScoreLog, Long> {

    Optional<TrustScoreLog> findByVote(Vote vote);

    /**
     * Loads all trust-score logs for the given vote ids in one query, used to
     * batch the dashboard history lookup instead of one query per vote.
     *
     * @param voteIds the vote ids to load logs for
     * @return logs whose vote id is in the set
     */
    List<TrustScoreLog> findByVote_IdIn(Collection<Long> voteIds);
}
