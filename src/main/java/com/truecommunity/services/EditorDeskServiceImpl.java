package com.truecommunity.services;

import com.truecommunity.dto.CommentInsight;
import com.truecommunity.dto.CommunityExpertVote;
import com.truecommunity.dto.ConsensusResponse;
import com.truecommunity.dto.ReviewItem;
import com.truecommunity.dto.TopicConsensusResponse;
import com.truecommunity.model.Evidence;
import com.truecommunity.model.Statement;
import com.truecommunity.model.Topic;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only editor desk assembler. It converts claimable and claimed statements
 * into review items and builds per-statement maps for comments, trusted votes,
 * topic suggestions, and evidence used by the editor workspace.
 */
@Service
@Transactional(readOnly = true)
public class EditorDeskServiceImpl implements EditorDeskService {

    private final StatementService statementService;
    private final VoteService voteService;
    private final CommentService commentService;

    public EditorDeskServiceImpl(StatementService statementService,
                                 VoteService voteService,
                                 CommentService commentService) {
        this.statementService = statementService;
        this.voteService = voteService;
        this.commentService = commentService;
    }

    @Override
    public List<ReviewItem> getAvailableReviewItems() {
        return toReviewItems(statementService.getAvailableClaims());
    }

    @Override
    public List<ReviewItem> getMyTaskReviewItems(User editor) {
        return toReviewItems(statementService.getMyClaimedTasks(editor));
    }

    /** Converts statements to review items, batching their consensus into one grouped query. */
    private List<ReviewItem> toReviewItems(List<Statement> statements) {
        Map<Long, ConsensusResponse> consensusByStatement = voteService.getConsensusByStatementIds(
                statements.stream().map(Statement::getId).toList());
        return statements.stream()
                .map(statement -> toReviewItem(statement, consensusByStatement.get(statement.getId())))
                .toList();
    }

    @Override
    public Map<Long, List<CommentInsight>> getTopComments(List<ReviewItem> tasks) {
        return byStatement(tasks, item -> commentService.getTop5Comments(item.getId()));
    }

    @Override
    public Map<Long, List<CommentInsight>> getBottomComments(List<ReviewItem> tasks) {
        return byStatement(tasks, item -> commentService.getBottom5Comments(item.getId()));
    }

    @Override
    public Map<Long, List<CommunityExpertVote>> getTopTrustVotes(List<ReviewItem> tasks) {
        return byStatement(tasks, item -> voteService.getTopTrustUserVotes(item.getId()));
    }

    @Override
    public Map<Long, List<TopicConsensusResponse>> getTopicSuggestions(List<ReviewItem> tasks) {
        return byStatement(tasks, item -> voteService.getTopicConsensus(item.getId()));
    }

    @Override
    public Map<Long, List<Evidence>> getEvidence(List<ReviewItem> tasks) {
        return byStatement(tasks, item -> voteService.getEvidenceForStatement(item.getId()));
    }

    /** Builds an insertion-ordered map keyed by statement id from the given review items. */
    private <T> Map<Long, T> byStatement(List<ReviewItem> tasks, Function<ReviewItem, T> mapper) {
        return tasks.stream().collect(Collectors.toMap(
                ReviewItem::getId,
                mapper,
                (first, second) -> first,
                LinkedHashMap::new
        ));
    }

    private ReviewItem toReviewItem(Statement statement, ConsensusResponse consensus) {
        Map<Verdict, Integer> percentByVerdict = new LinkedHashMap<>();
        Map<Verdict, Long> countByVerdict = new LinkedHashMap<>();
        for (Verdict verdict : Verdict.values()) {
            Double pct = consensus.getPercentagesByVerdict().get(verdict);
            Long count = consensus.getCountsByVerdict().get(verdict);
            percentByVerdict.put(verdict, pct == null ? 0 : (int) Math.round(pct));
            countByVerdict.put(verdict, count == null ? 0L : count);
        }

        return new ReviewItem(
                statement.getId(),
                statement.getText(),
                statement.getSpeaker(),
                statement.getSpeakerName(),
                statement.getSourceUrl(),
                statement.getStatus(),
                statement.getTopics().stream().map(Topic::getName).collect(Collectors.toCollection(LinkedHashSet::new)),
                consensus.getTotalVotes(),
                consensus.leadingVerdict(),
                percentByVerdict,
                countByVerdict,
                consensus
        );
    }

}
