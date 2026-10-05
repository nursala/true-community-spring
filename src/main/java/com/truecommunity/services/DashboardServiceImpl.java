package com.truecommunity.services;

import com.truecommunity.dto.DashboardView;
import com.truecommunity.dto.EditorStatsView;
import com.truecommunity.dto.HistoryOutcome;
import com.truecommunity.dto.HistoryRow;
import com.truecommunity.model.EditorDecision;
import com.truecommunity.model.Role;
import com.truecommunity.model.Statement;
import com.truecommunity.model.TrustScoreLog;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;
import com.truecommunity.model.Vote;
import com.truecommunity.repo.EditorDecisionRepository;
import com.truecommunity.repo.TrustScoreLogRepository;
import com.truecommunity.repo.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only dashboard assembler. It computes headline voting accuracy over all
 * of the user's votes, applies filters only to displayed history rows, and adds
 * editor decision statistics when the viewer is an editor.
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final VoteRepository voteRepository;

    private final EditorDecisionRepository editorDecisionRepository;

    private final TrustScoreLogRepository trustScoreLogRepository;

    public DashboardServiceImpl(VoteRepository voteRepository,
                                EditorDecisionRepository editorDecisionRepository,
                                TrustScoreLogRepository trustScoreLogRepository) {
        this.voteRepository = voteRepository;
        this.editorDecisionRepository = editorDecisionRepository;
        this.trustScoreLogRepository = trustScoreLogRepository;
    }

    /**
     * Builds the dashboard for the supplied user. Summary counts use all votes;
     * filters apply only to the visible history section.
     *
     * @param user the dashboard owner
     * @param outcome optional outcome filter
     * @param verdict optional verdict filter
     * @return the assembled dashboard view
     */
    @Override
    public DashboardView buildDashboard(User user, HistoryOutcome outcome, Verdict verdict) {
        List<Vote> votes = voteRepository.findByUser(user);
        votes.sort(Comparator.comparing(Vote::getCreatedAt).reversed());

        // Batch the two per-vote lookups into one query each: the editor decision per
        // voted statement, and the trust-score log per vote. Avoids the previous
        // O(votes) round-trips while producing identical rows.
        Map<Long, Verdict> verdictByStatementId = decisionsByStatementId(votes);
        Map<Long, TrustScoreLog> logByVoteId = logsByVoteId(votes);

        List<HistoryRow> history = new ArrayList<>();
        int decidedCount = 0;
        int matchCount = 0;

        for (Vote vote : votes) {
            Statement statement = vote.getStatement();
            Verdict editorVerdict = verdictByStatementId.get(statement.getId());

            boolean decided = editorVerdict != null;
            boolean match = decided && editorVerdict == vote.getVerdict();
            TrustScoreLog scoreLog = logByVoteId.get(vote.getId());

            if (decided) {
                decidedCount++;
                if (match) {
                    matchCount++;
                }
            }

            history.add(new HistoryRow(
                    statement.getId(),
                    statement.getText(),
                    vote.getVerdict(),
                    editorVerdict,
                    scoreLog == null ? null : scoreLog.getChangeAmount(),
                    scoreLog == null ? null : scoreLog.getReason(),
                    vote.getCreatedAt()
            ));
        }

        int accuracyPercent = decidedCount == 0 ? 0 : (int) Math.round(matchCount * 100.0 / decidedCount);

        // Headline stats reflect ALL votes; the filter only narrows the displayed rows.
        List<HistoryRow> displayedHistory = filterHistory(history, outcome, verdict);
        EditorStatsView editorStats = user.getRole() == Role.EDITOR ? buildEditorStats(user) : null;

        return new DashboardView(
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getTrustScore() == null ? 0.0 : user.getTrustScore(),
                votes.size(),
                decidedCount,
                matchCount,
                accuracyPercent,
                displayedHistory,
                editorStats
        );
    }

    /** Loads the editor verdict per voted statement in one query, keyed by statement id. */
    private Map<Long, Verdict> decisionsByStatementId(List<Vote> votes) {
        List<Long> statementIds = votes.stream().map(vote -> vote.getStatement().getId()).distinct().toList();
        if (statementIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Verdict> verdictByStatementId = new HashMap<>();
        for (EditorDecision decision : editorDecisionRepository.findByStatement_IdIn(statementIds)) {
            verdictByStatementId.put(decision.getStatement().getId(), decision.getFinalVerdict());
        }
        return verdictByStatementId;
    }

    /** Loads the trust-score log per vote in one query, keyed by vote id. */
    private Map<Long, TrustScoreLog> logsByVoteId(List<Vote> votes) {
        List<Long> voteIds = votes.stream().map(Vote::getId).toList();
        if (voteIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, TrustScoreLog> logByVoteId = new HashMap<>();
        for (TrustScoreLog log : trustScoreLogRepository.findByVote_IdIn(voteIds)) {
            if (log.getVote() != null) {
                logByVoteId.put(log.getVote().getId(), log);
            }
        }
        return logByVoteId;
    }

    /** Applies the outcome/verdict filter to the history rows (display only). */
    private List<HistoryRow> filterHistory(List<HistoryRow> history, HistoryOutcome outcome, Verdict verdict) {
        HistoryOutcome effectiveOutcome = outcome == null ? HistoryOutcome.ALL : outcome;
        List<HistoryRow> filtered = new ArrayList<>();
        for (HistoryRow row : history) {
            boolean outcomeMatches = effectiveOutcome == HistoryOutcome.ALL || row.getOutcome() == effectiveOutcome;
            boolean verdictMatches = verdict == null || row.getUserVerdict() == verdict;
            if (outcomeMatches && verdictMatches) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    /** Builds the editor-only panel: total decisions and an ordered per-verdict distribution. */
    private EditorStatsView buildEditorStats(User editor) {
        List<EditorDecision> decisions = editorDecisionRepository.findByEditorOrderByDecidedAtDesc(editor);
        Map<Verdict, Integer> distribution = new LinkedHashMap<>();
        for (Verdict verdict : Verdict.values()) {
            distribution.put(verdict, 0);
        }
        for (EditorDecision decision : decisions) {
            distribution.merge(decision.getFinalVerdict(), 1, Integer::sum);
        }
        return new EditorStatsView(decisions.size(), distribution);
    }
}
