package com.truecommunity.services;

import com.truecommunity.dto.FeedView;
import com.truecommunity.dto.StatementSearchForm;
import com.truecommunity.model.User;

/**
 * Assembles the discovery feed read-model (available/pending/resolved buckets
 * and the per-statement consensus snapshot), keeping it out of the controller.
 */
public interface FeedService {

    /** Builds the feed view-model. {@code currentUser} may be null for anonymous visitors. */
    FeedView buildFeed(User currentUser, StatementSearchForm searchForm);
}
