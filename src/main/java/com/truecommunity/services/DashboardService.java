package com.truecommunity.services;

import com.truecommunity.dto.DashboardView;
import com.truecommunity.dto.HistoryOutcome;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;

/**
 * Builds dashboard read-models for a user's voting history, trust-score
 * outcomes, and editor-only decision statistics.
 */
public interface DashboardService {

    /**
     * Builds the dashboard view. Headline stats are always computed over ALL of the
     * user's votes; the outcome/verdict filter only narrows the displayed history rows.
     */
    DashboardView buildDashboard(User user, HistoryOutcome outcome, Verdict verdict);
}
