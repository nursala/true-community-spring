package com.truecommunity.services;

import com.truecommunity.model.Statement;
import com.truecommunity.model.TrustScoreLog;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;
import com.truecommunity.model.Vote;
import com.truecommunity.repo.TrustScoreLogRepository;
import com.truecommunity.repo.UserRepository;
import com.truecommunity.repo.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Transactional trust-score implementation. It records a log entry for each
 * applied vote adjustment so a later decision revert can undo exactly those
 * changes.
 */
@Service
@Transactional
public class TrustScoreServiceImpl implements TrustScoreService {

    private static final double CORRECT_VOTE_DELTA = 0.10;

    private static final double WRONG_VOTE_DELTA = -0.05;

    private static final String CORRECT_VOTE_REASON = "Matched";

    private static final String WRONG_VOTE_REASON = "Did not match";

    private final VoteRepository voteRepository;

    private final UserRepository userRepository;

    private final TrustScoreLogRepository trustScoreLogRepository;

    public TrustScoreServiceImpl(VoteRepository voteRepository,
                                 UserRepository userRepository,
                                 TrustScoreLogRepository trustScoreLogRepository) {
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.trustScoreLogRepository = trustScoreLogRepository;
    }

    /**
     * Adjusts every voter on the statement based on whether their vote matched
     * the final verdict, persists the users, and creates trust score logs.
     *
     * @param statement the decided statement
     * @param finalVerdict the editor's final verdict
     * @return ids of voters whose score changed
     */
    @Override
    public List<Long> updateTrustScoresForDecision(Statement statement, Verdict finalVerdict) {
        List<Vote> votes = voteRepository.findByStatement(statement);
        if (votes.isEmpty()) {
            return List.of();
        }

        List<User> updatedUsers = new ArrayList<>();
        List<TrustScoreLog> logs = new ArrayList<>();
        List<Long> affectedUserIds = new ArrayList<>();
        for (Vote vote : votes) {
            User voter = vote.getUser();
            boolean matched = vote.getVerdict() == finalVerdict;
            double delta = matched ? CORRECT_VOTE_DELTA : WRONG_VOTE_DELTA;
            String reason = matched ? CORRECT_VOTE_REASON : WRONG_VOTE_REASON;

            voter.adjustTrustScore(delta);
            updatedUsers.add(voter);
            affectedUserIds.add(voter.getId());
            logs.add(TrustScoreLog.create(voter, vote, delta, voter.getTrustScore(), reason));
        }

        userRepository.saveAll(updatedUsers);
        trustScoreLogRepository.saveAll(logs);
        return affectedUserIds;
    }

    /**
     * Uses stored trust-score logs to reverse prior adjustments and then
     * deletes those logs.
     *
     * @param statement the statement whose decision is being reverted
     * @return ids of voters whose score changed
     */
    @Override
    public List<Long> revertTrustScoresForDecision(Statement statement) {
        List<User> updatedUsers = new ArrayList<>();
        List<TrustScoreLog> logsToDelete = new ArrayList<>();
        List<Long> affectedUserIds = new ArrayList<>();

        for (Vote vote : voteRepository.findByStatement(statement)) {
            trustScoreLogRepository.findByVote(vote).ifPresent(log -> {
                log.getUser().adjustTrustScore(-log.getChangeAmount());
                updatedUsers.add(log.getUser());
                affectedUserIds.add(log.getUser().getId());
                logsToDelete.add(log);
            });
        }

        if (!updatedUsers.isEmpty()) {
            userRepository.saveAll(updatedUsers);
            trustScoreLogRepository.deleteAll(logsToDelete);
        }
        return affectedUserIds;
    }
}
