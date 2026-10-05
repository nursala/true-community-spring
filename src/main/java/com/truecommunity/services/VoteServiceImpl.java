package com.truecommunity.services;

import com.truecommunity.dto.CommunityExpertVote;
import com.truecommunity.dto.ConsensusResponse;
import com.truecommunity.dto.TopicConsensusResponse;
import com.truecommunity.dto.VoteRequest;
import com.truecommunity.dto.VoteResponse;
import com.truecommunity.exception.ConsensusAccessDeniedException;
import com.truecommunity.exception.InvalidStatementStateException;
import com.truecommunity.exception.InvalidTopicSelectionException;
import com.truecommunity.exception.InvalidUrlException;
import com.truecommunity.exception.StatementNotFoundException;
import com.truecommunity.exception.VoteAlreadyExistsException;
import com.truecommunity.exception.VotingClosedException;
import com.truecommunity.model.Comment;
import com.truecommunity.model.Evidence;
import com.truecommunity.model.Role;
import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.Topic;
import com.truecommunity.model.User;
import com.truecommunity.model.UserActivityType;
import com.truecommunity.model.Verdict;
import com.truecommunity.model.Vote;
import com.truecommunity.repo.CommentRepository;
import com.truecommunity.repo.EvidenceRepository;
import com.truecommunity.repo.StatementRepository;
import com.truecommunity.repo.TopicRepository;
import com.truecommunity.repo.UserRepository;
import com.truecommunity.repo.VoteRepository;
import com.truecommunity.security.AuthenticatedUserProvider;
import com.truecommunity.util.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transactional implementation of vote submission and consensus queries.
 * It prevents duplicate votes, blocks voting after decisions or while a
 * statement is claimed, enforces anti-bias access for aggregate data, and
 * publishes {@code VOTE_SUBMITTED} SSE events only after a successful commit.
 */
@Service
@Transactional
public class VoteServiceImpl implements VoteService {

    private static final Logger log = LoggerFactory.getLogger(VoteServiceImpl.class);

    private final VoteRepository voteRepository;

    private final EvidenceRepository evidenceRepository;

    private final UserRepository userRepository;

    private final AuthenticatedUserProvider authenticatedUserProvider;

    private final StatementRepository statementRepository;

    private final TopicRepository topicRepository;

    private final UserActivityLogService userActivityLogService;

    private final CommentRepository commentRepository;

    private final SseEventService sseEventService;

    public VoteServiceImpl(VoteRepository voteRepository,
                           EvidenceRepository evidenceRepository,
                           UserRepository userRepository,
                           AuthenticatedUserProvider authenticatedUserProvider,
                           StatementRepository statementRepository,
                           TopicRepository topicRepository,
                           UserActivityLogService userActivityLogService,
                           CommentRepository commentRepository,
                           SseEventService sseEventService) {
        this.voteRepository = voteRepository;
        this.evidenceRepository = evidenceRepository;
        this.userRepository = userRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.statementRepository = statementRepository;
        this.topicRepository = topicRepository;
        this.userActivityLogService = userActivityLogService;
        this.commentRepository = commentRepository;
        this.sseEventService = sseEventService;
    }

    /**
     * Saves one authenticated user's vote and optional evidence/comment side
     * effects in the surrounding transaction. Duplicate votes, decided
     * statements, claimed statements, and invalid topic suggestions are rejected
     * before persistence.
     *
     * @param statementId the statement being voted on
     * @param request the submitted vote fields
     * @param authentication the current authentication used to resolve the voter
     * @return the saved vote response
     */
    @Override
    public VoteResponse submitVote(Long statementId, VoteRequest request, Authentication authentication) {
        User user = authenticatedUserProvider.getAuthenticatedUser(authentication);
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + statementId));

        if (statement.getStatus() == StatementStatus.DECIDED) {
            throw new VotingClosedException("ההצבעה על אמירה זו נסגרה לאחר פרסום הכרעה סופית.");
        }

        if (statement.getClaimedBy() != null) {
            throw new InvalidStatementStateException("לא ניתן להצביע בזמן שהאמירה נמצאת בבדיקת עורך.");
        }

        if (voteRepository.existsByUserAndStatement(user, statement)) {
            throw new VoteAlreadyExistsException("כבר הצבעת על אמירה זו.");
        }

        Topic suggestedTopic = resolveSuggestedTopic(request.getSuggestedTopicId());

        String evidenceUrl = sanitizeUrl(request.getEvidenceUrl());
        Vote vote = Vote.create(
                user,
                statement,
                request.getVerdict(),
                request.getExplanation(),
                evidenceUrl
        );
        vote.setSuggestedTopic(suggestedTopic);

        Vote savedVote;
        try {
            // Flush immediately so a concurrent duplicate that slipped past the
            // exists check is caught here via the DB unique constraint
            // (uk_votes_user_statement) instead of failing at commit as a 500.
            savedVote = voteRepository.saveAndFlush(vote);
        } catch (DataIntegrityViolationException ex) {
            throw new VoteAlreadyExistsException("כבר הצבעת על אמירה זו.");
        }

        userActivityLogService.log(user, UserActivityType.VOTED, statement, savedVote.getId());

        if (evidenceUrl != null) {
            String evidenceDescription = hasText(request.getEvidenceDescription())
                    ? request.getEvidenceDescription().trim()
                    : "Evidence link shared with the vote.";
            Evidence savedEvidence = evidenceRepository.save(Evidence.create(
                    savedVote,
                    evidenceUrl,
                    evidenceDescription
            ));
            userActivityLogService.log(user, UserActivityType.ADDED_EVIDENCE, statement, savedEvidence.getId());
        }

        // Publish the vote's reason and/or evidence link as a public comment for discussion.
        String commentContent = buildCommentFromVote(request);
        if (commentContent != null) {
            Comment savedComment = commentRepository.save(Comment.create(user, statement, commentContent, evidenceUrl));
            userActivityLogService.log(user, UserActivityType.COMMENTED, statement, savedComment.getId());
        }

        publishVoteSubmittedAfterCommit(statementId);
        return toVoteResponse(savedVote);
    }

    /**
     * Builds aggregate verdict counts without viewer gating for internal/editor
     * flows that are already authorized.
     *
     * @param statementId the statement whose votes should be counted
     * @return verdict counts and percentages
     */
    @Override
    public ConsensusResponse getConsensus(Long statementId) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + statementId));
        return buildConsensus(statement);
    }

    /**
     * Applies the anti-bias rule before returning consensus. Editors may view
     * the data directly; normal users must have a saved vote for the statement.
     *
     * @param statementId the statement whose votes should be counted
     * @param user the viewer, or {@code null} for anonymous access
     * @return verdict counts and percentages visible to the viewer
     */
    @Override
    public ConsensusResponse getConsensusForViewer(Long statementId, User user) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + statementId));
        requireConsensusAccess(statement, user);
        return buildConsensus(statement);
    }

    /**
     * Resolves the authenticated viewer and delegates to the user-based
     * consensus access check.
     *
     * @param statementId the statement whose votes should be counted
     * @param authentication the current authentication
     * @return verdict counts and percentages visible to the viewer
     */
    @Override
    public ConsensusResponse getConsensusForViewer(Long statementId, Authentication authentication) {
        return getConsensusForViewer(statementId, currentUserOrNull(authentication));
    }

    private ConsensusResponse buildConsensus(Statement statement) {
        Map<Verdict, Long> rawCounts = new EnumMap<>(Verdict.class);
        for (Verdict verdict : Verdict.values()) {
            rawCounts.put(verdict, voteRepository.countByStatementAndVerdict(statement, verdict));
        }
        return assembleConsensus(statement.getId(), rawCounts);
    }

    /**
     * Builds raw consensus for many statements from a single grouped verdict-count
     * query. Callers must have already resolved viewer access for the whole set;
     * the values produced are byte-identical to {@link #buildConsensus(Statement)}.
     *
     * @param statementIds the statement ids to build consensus for
     * @return an insertion-ordered map of statement id to consensus
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, ConsensusResponse> getConsensusByStatementIds(Collection<Long> statementIds) {
        Map<Long, ConsensusResponse> result = new LinkedHashMap<>();
        if (statementIds == null || statementIds.isEmpty()) {
            return result;
        }

        // One grouped query for all ids; rows only exist for (statement, verdict) pairs with votes.
        Map<Long, Map<Verdict, Long>> countsByStatement = new HashMap<>();
        for (Object[] row : voteRepository.countVerdictsGroupedByStatementIds(statementIds)) {
            Long statementId = (Long) row[0];
            Verdict verdict = (Verdict) row[1];
            long count = ((Number) row[2]).longValue();
            countsByStatement.computeIfAbsent(statementId, ignored -> new EnumMap<>(Verdict.class))
                    .put(verdict, count);
        }

        // Preserve caller ordering and guarantee an entry (all-zero if needed) per requested id.
        for (Long statementId : statementIds) {
            result.put(statementId, assembleConsensus(statementId,
                    countsByStatement.getOrDefault(statementId, Map.of())));
        }
        return result;
    }

    /** Fills every verdict (zeros included), totals, and percentages into a consensus response. */
    private ConsensusResponse assembleConsensus(Long statementId, Map<Verdict, Long> rawCounts) {
        Map<Verdict, Long> countsByVerdict = new EnumMap<>(Verdict.class);
        Map<Verdict, Double> percentagesByVerdict = new EnumMap<>(Verdict.class);

        long totalVotes = 0;
        for (Verdict verdict : Verdict.values()) {
            long count = rawCounts.getOrDefault(verdict, 0L);
            countsByVerdict.put(verdict, count);
            totalVotes += count;
        }
        for (Verdict verdict : Verdict.values()) {
            percentagesByVerdict.put(verdict, calculatePercentage(countsByVerdict.get(verdict), totalVotes));
        }

        ConsensusResponse response = new ConsensusResponse();
        response.setStatementId(statementId);
        response.setTotalVotes(totalVotes);
        response.setCountsByVerdict(countsByVerdict);
        response.setPercentagesByVerdict(percentagesByVerdict);
        return response;
    }

    /**
     * Returns topic suggestion counts for internal/editor flows where aggregate
     * access has already been authorized.
     *
     * @param statementId the statement whose suggested topics should be counted
     * @return topic names with vote counts
     */
    @Override
    public List<TopicConsensusResponse> getTopicConsensus(Long statementId) {
        if (!statementRepository.existsById(statementId)) {
            throw new StatementNotFoundException("Statement not found with id: " + statementId);
        }

        return buildTopicConsensus(statementId);
    }

    /**
     * Applies the same anti-bias gate used for verdict consensus before
     * returning vote-derived topic suggestions.
     *
     * @param statementId the statement whose suggested topics should be counted
     * @param authentication the current authentication
     * @return topic names with vote counts visible to the viewer
     */
    @Override
    public List<TopicConsensusResponse> getTopicConsensusForViewer(Long statementId, Authentication authentication) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + statementId));
        requireConsensusAccess(statement, currentUserOrNull(authentication));
        return buildTopicConsensus(statementId);
    }

    private List<TopicConsensusResponse> buildTopicConsensus(Long statementId) {
        return voteRepository.countVotesPerTopicForStatement(statementId).stream()
                .map(row -> new TopicConsensusResponse((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    /**
     * Returns the highest-trust voters' public vote summaries for editor review.
     *
     * @param statementId the statement being reviewed
     * @return at most five vote summaries
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommunityExpertVote> getTopTrustUserVotes(Long statementId) {
        return voteRepository.findTopTrustUserVotesByStatementId(statementId, PageRequest.of(0, 5));
    }

    /**
     * Loads evidence records attached to votes for the given statement.
     *
     * @param statementId the statement whose vote evidence should be loaded
     * @return evidence records for the statement
     */
    @Override
    @Transactional(readOnly = true)
    public List<Evidence> getEvidenceForStatement(Long statementId) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new StatementNotFoundException("Statement not found with id: " + statementId));
        return evidenceRepository.findByVote_Statement(statement);
    }

    /**
     * Checks whether a user has voted without exposing vote contents.
     *
     * @param statementId the statement id
     * @param userId the user id
     * @return {@code true} when a vote exists
     */
    @Override
    public boolean hasUserAlreadyVoted(Long statementId, Long userId) {
        return userRepository.findById(userId)
                .flatMap(user -> statementRepository.findById(statementId)
                        .map(statement -> voteRepository.existsByUserAndStatement(user, statement)))
                .orElse(false);
    }

    private void requireConsensusAccess(Statement statement, User user) {
        if (user == null
                || (user.getRole() != Role.EDITOR && !voteRepository.existsByUserAndStatement(user, statement))) {
            throw new ConsensusAccessDeniedException(
                    "יש להצביע על האמירה לפני צפייה בקונצנזוס הקהילתי.");
        }
    }

    private User currentUserOrNull(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authenticatedUserProvider.getAuthenticatedUser(authentication);
    }

    private Topic resolveSuggestedTopic(Long topicId) {
        // Crowdsourced classification: every vote must suggest exactly one valid topic.
        if (topicId == null) {
            throw new InvalidTopicSelectionException("יש לבחור נושא מוצע.");
        }
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new InvalidTopicSelectionException("הנושא שנבחר אינו קיים."));
    }

    private VoteResponse toVoteResponse(Vote vote) {
        VoteResponse response = new VoteResponse();
        response.setVoteId(vote.getId());
        response.setStatementId(vote.getStatement().getId());
        response.setUserId(vote.getUser().getId());
        response.setVerdict(vote.getVerdict());
        response.setEvidenceUrl(vote.getEvidenceUrl());
        if (vote.getSuggestedTopic() != null) {
            response.setSuggestedTopicId(vote.getSuggestedTopic().getId());
            response.setSuggestedTopicName(vote.getSuggestedTopic().getName());
        }
        response.setCreatedAt(vote.getCreatedAt());
        return response;
    }

    private void publishVoteSubmittedAfterCommit(Long statementId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("Skipping VOTE_SUBMITTED SSE for statement {} because no transaction synchronization is active.",
                    statementId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishVoteSubmitted(statementId);
            }
        });
    }

    private void publishVoteSubmitted(Long statementId) {
        Map<String, Long> payload = Map.of("statementId", statementId);
        sseEventService.publish(SseChannels.statement(statementId), SseEvents.VOTE_SUBMITTED, payload);
        sseEventService.publish(SseChannels.PUBLIC_FEED, SseEvents.VOTE_SUBMITTED, payload);
        sseEventService.publish(SseChannels.EDITOR_DESK, SseEvents.VOTE_SUBMITTED, payload);
    }

    /** Creates the public discussion comment associated with a vote when content or evidence exists. */
    private String buildCommentFromVote(VoteRequest request) {
        if (hasText(request.getExplanation())) {
            String content = request.getExplanation().trim();
            return content.length() > 1000 ? content.substring(0, 1000) : content;
        }
        return hasText(request.getEvidenceUrl()) ? "Evidence link attached to this vote." : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Normalizes an optional evidence URL, rejecting non-http/https schemes. */
    private String sanitizeUrl(String value) {
        try {
            return UrlValidator.sanitizeOptional(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidUrlException("יש להזין קישור ראיה בפורמט http או https בלבד.");
        }
    }

    private double calculatePercentage(long count, long totalVotes) {
        if (totalVotes == 0) {
            return 0.0;
        }

        return (count * 100.0) / totalVotes;
    }
}
