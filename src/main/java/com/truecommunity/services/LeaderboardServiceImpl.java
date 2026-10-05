package com.truecommunity.services;

import com.truecommunity.dto.LeaderboardResponse;
import com.truecommunity.dto.LeaderboardUserResponse;
import com.truecommunity.model.Role;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;
import com.truecommunity.repo.EditorDecisionRepository;
import com.truecommunity.repo.UserRepository;
import com.truecommunity.repo.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only leaderboard implementation. It builds stable rankings from bulk
 * repository queries so vote and decision aggregates do not require per-user
 * database lookups.
 */
@Service
@Transactional(readOnly = true)
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final int TOP_SIZE = 10;
    private static final String SORT_VOTES = "votes";
    private static final String SORT_ACCURACY = "accuracy";
    private static final String SORT_TRUST = "trust";
    private static final String SCOPE_ALL = "all";
    private static final String SCOPE_TOP = "top";

    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final EditorDecisionRepository editorDecisionRepository;

    public LeaderboardServiceImpl(UserRepository userRepository,
                                  VoteRepository voteRepository,
                                  EditorDecisionRepository editorDecisionRepository) {
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.editorDecisionRepository = editorDecisionRepository;
    }

    /**
     * Builds a ranked leaderboard using the requested sort/scope and marks the
     * current user's entry when available.
     *
     * @param sort ranking metric: trust, votes, or accuracy
     * @param scope top entries or all entries
     * @param currentUser the authenticated viewer, or {@code null}
     * @return the leaderboard response
     */
    @Override
    public LeaderboardResponse build(String sort, String scope, User currentUser) {
        String effectiveSort = normalizeSort(sort);
        String effectiveScope = SCOPE_ALL.equalsIgnoreCase(scope) ? SCOPE_ALL : SCOPE_TOP;

        // One-pass aggregation: 3 bulk queries, no per-user query (no N+1).
        Map<Long, Verdict> finalVerdictByStatement = loadFinalVerdicts();
        Map<Long, int[]> aggregateByUser = aggregateVotes(finalVerdictByStatement);

        Long currentUserId = currentUser == null ? null : currentUser.getId();

        List<LeaderboardUserResponse> ranked = new ArrayList<>();
        for (User user : userRepository.findByRoleNot(Role.EDITOR)) {
            int[] agg = aggregateByUser.getOrDefault(user.getId(), new int[3]);
            ranked.add(toEntry(user, agg, currentUserId));
        }

        ranked.sort(comparatorFor(effectiveSort));
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).setRank(i + 1);
        }

        // "me" comes from the FULL ranking, so the rank is correct even outside the top slice.
        LeaderboardUserResponse me = currentUserId == null ? null : ranked.stream()
                .filter(entry -> currentUserId.equals(entry.getId()))
                .findFirst()
                .orElse(null);

        List<LeaderboardUserResponse> entries = SCOPE_ALL.equals(effectiveScope)
                ? ranked
                : ranked.subList(0, Math.min(TOP_SIZE, ranked.size()));

        return new LeaderboardResponse(effectiveSort, effectiveScope, new ArrayList<>(entries), me);
    }

    private String normalizeSort(String sort) {
        if (SORT_VOTES.equalsIgnoreCase(sort)) {
            return SORT_VOTES;
        }
        if (SORT_ACCURACY.equalsIgnoreCase(sort)) {
            return SORT_ACCURACY;
        }
        return SORT_TRUST;
    }

    private Map<Long, Verdict> loadFinalVerdicts() {
        Map<Long, Verdict> finalVerdictByStatement = new HashMap<>();
        for (Object[] row : editorDecisionRepository.findStatementFinalVerdicts()) {
            finalVerdictByStatement.put((Long) row[0], (Verdict) row[1]);
        }
        return finalVerdictByStatement;
    }

    /** Returns userId -> [totalVotes, decidedVotes, matchedVotes] in a single pass. */
    private Map<Long, int[]> aggregateVotes(Map<Long, Verdict> finalVerdictByStatement) {
        Map<Long, int[]> aggregateByUser = new HashMap<>();
        for (Object[] row : voteRepository.findVoteAggregationTuples()) {
            Long userId = (Long) row[0];
            Long statementId = (Long) row[1];
            Verdict verdict = (Verdict) row[2];

            int[] agg = aggregateByUser.computeIfAbsent(userId, key -> new int[3]);
            agg[0]++;
            Verdict finalVerdict = finalVerdictByStatement.get(statementId);
            if (finalVerdict != null) {
                agg[1]++;
                if (finalVerdict == verdict) {
                    agg[2]++;
                }
            }
        }
        return aggregateByUser;
    }

    private LeaderboardUserResponse toEntry(User user, int[] agg, Long currentUserId) {
        int total = agg[0];
        int decided = agg[1];
        int matched = agg[2];
        int accuracyPercent = decided == 0 ? 0 : (int) Math.round(matched * 100.0 / decided);

        LeaderboardUserResponse entry = new LeaderboardUserResponse();
        entry.setId(user.getId());
        entry.setUsername(user.getUsername());
        entry.setRole(user.getRole().name());
        entry.setTrustScore(user.getTrustScore() == null ? 0.0 : user.getTrustScore());
        entry.setTotalVotes(total);
        entry.setAccuracyPercent(accuracyPercent);
        entry.setCurrentUser(currentUserId != null && currentUserId.equals(user.getId()));
        return entry;
    }

    private Comparator<LeaderboardUserResponse> comparatorFor(String sort) {
        Comparator<LeaderboardUserResponse> primary = switch (sort) {
            case SORT_VOTES -> Comparator.comparingInt(LeaderboardUserResponse::getTotalVotes).reversed();
            case SORT_ACCURACY -> Comparator.comparingInt(LeaderboardUserResponse::getAccuracyPercent).reversed();
            default -> Comparator.comparingDouble(LeaderboardServiceImpl::trustOf).reversed();
        };
        // Deterministic tiebreakers so ranks are stable across requests.
        return primary
                .thenComparing(Comparator.comparingDouble(LeaderboardServiceImpl::trustOf).reversed())
                .thenComparing(entry -> entry.getUsername() == null ? "" : entry.getUsername(),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(LeaderboardUserResponse::getId);
    }

    private static double trustOf(LeaderboardUserResponse entry) {
        return entry.getTrustScore() == null ? 0.0 : entry.getTrustScore();
    }
}
