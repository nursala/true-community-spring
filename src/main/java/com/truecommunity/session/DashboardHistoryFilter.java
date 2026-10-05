package com.truecommunity.session;

import com.truecommunity.dto.HistoryOutcome;
import com.truecommunity.model.Verdict;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

/**
 * Session-scoped state: the last-applied filter for the dashboard history table.
 * Lives for the duration of the HTTP session and is cleared on logout/expiry.
 */
@Component
@SessionScope
public class DashboardHistoryFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    private HistoryOutcome outcome = HistoryOutcome.ALL;

    // null means "all verdicts".
    private Verdict verdict;

    public HistoryOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(HistoryOutcome outcome) {
        this.outcome = outcome == null ? HistoryOutcome.ALL : outcome;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }
}
