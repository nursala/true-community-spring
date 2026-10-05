package com.truecommunity.services;

import com.truecommunity.dto.CommentInsight;
import com.truecommunity.dto.CommunityExpertVote;
import com.truecommunity.dto.ReviewItem;
import com.truecommunity.dto.TopicConsensusResponse;
import com.truecommunity.model.Evidence;
import com.truecommunity.model.User;

import java.util.List;
import java.util.Map;

/**
 * Builds the read-model the editor desk view needs (review items and the
 * per-statement analytics maps), keeping that aggregation out of the controller.
 */
public interface EditorDeskService {

    List<ReviewItem> getAvailableReviewItems();

    List<ReviewItem> getMyTaskReviewItems(User editor);

    Map<Long, List<CommentInsight>> getTopComments(List<ReviewItem> tasks);

    Map<Long, List<CommentInsight>> getBottomComments(List<ReviewItem> tasks);

    Map<Long, List<CommunityExpertVote>> getTopTrustVotes(List<ReviewItem> tasks);

    Map<Long, List<TopicConsensusResponse>> getTopicSuggestions(List<ReviewItem> tasks);

    Map<Long, List<Evidence>> getEvidence(List<ReviewItem> tasks);
}
