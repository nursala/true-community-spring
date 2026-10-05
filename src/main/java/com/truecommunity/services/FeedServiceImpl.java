package com.truecommunity.services;

import com.truecommunity.dto.ConsensusResponse;
import com.truecommunity.dto.FeedView;
import com.truecommunity.dto.StatementSearchForm;
import com.truecommunity.model.Role;
import com.truecommunity.model.Statement;
import com.truecommunity.model.User;
import com.truecommunity.repo.CommentRepository;
import com.truecommunity.repo.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only public feed assembler. It separates available, pending, and
 * resolved statements and only exposes vote counts for available statements to
 * editors, preserving the public anti-bias behavior.
 */
@Service
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

    private final StatementService statementService;
    private final VoteService voteService;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;

    public FeedServiceImpl(StatementService statementService, VoteService voteService,
                           VoteRepository voteRepository, CommentRepository commentRepository) {
        this.statementService = statementService;
        this.voteService = voteService;
        this.voteRepository = voteRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Builds the feed buckets and their supporting counts/consensus maps for
     * the current viewer and search form.
     *
     * @param currentUser the viewer, or {@code null} for anonymous access
     * @param searchForm optional feed filters and sorting
     * @return the assembled feed view
     */
    @Override
    public FeedView buildFeed(User currentUser, StatementSearchForm searchForm) {
        List<Statement> pendingItems = statementService.getPendingVerificationStatements(currentUser, searchForm);
        // Pending-verification items are, by construction, statements the viewer already voted on
        // (or the viewer is an editor), so consensus access is guaranteed for the whole bucket.
        // Build every item's consensus in one grouped query instead of one query per item.
        Map<Long, ConsensusResponse> pendingConsensus = voteService.getConsensusByStatementIds(
                pendingItems.stream().map(Statement::getId).toList());

        List<Statement> availableItems = statementService.getAvailableForVoting(currentUser, searchForm);
        List<Long> availableIds = availableItems.stream().map(Statement::getId).toList();
        // Guard against an empty IN () and skip the queries entirely when there is nothing to count.
        Map<Long, Long> availableVoteCounts = availableIds.isEmpty() || !canViewAvailableVoteCounts(currentUser)
                ? Map.of()
                : countsById(voteRepository.countGroupedByStatementIds(availableIds));
        Map<Long, Long> availableCommentCounts = availableIds.isEmpty()
                ? Map.of()
                : countsById(commentRepository.countGroupedByStatementIds(availableIds));

        return new FeedView(
                availableItems,
                pendingItems,
                pendingConsensus,
                statementService.getResolvedStatements(searchForm),
                statementService.getAllTopicNames(),
                availableVoteCounts,
                availableCommentCounts
        );
    }

    /** Turns grouped (statementId, count) rows into a map keyed by statement id. */
    private Map<Long, Long> countsById(List<Object[]> rows) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private boolean canViewAvailableVoteCounts(User currentUser) {
        return currentUser != null && currentUser.getRole() == Role.EDITOR;
    }
}
