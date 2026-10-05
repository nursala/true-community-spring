package com.truecommunity.dto;

import java.util.List;

/**
 * Result of a demotion operation, including released statement ids used for
 * after-commit SSE notifications by the controller.
 */
public record UserDemotionResult(Long userId, boolean roleChanged, List<Long> releasedStatementIds) {

    /**
     * Defensively copies released ids so callers cannot mutate the result.
     */
    public UserDemotionResult {
        releasedStatementIds = List.copyOf(releasedStatementIds);
    }
}
