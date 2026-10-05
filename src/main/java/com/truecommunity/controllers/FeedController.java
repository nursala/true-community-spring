package com.truecommunity.controllers;

import com.truecommunity.dto.FeedView;
import com.truecommunity.dto.StatementSearchForm;
import com.truecommunity.model.User;
import com.truecommunity.model.Verdict;
import com.truecommunity.services.FeedService;
import com.truecommunity.services.SseChannels;
import com.truecommunity.services.SseEventService;
import com.truecommunity.services.UserService;
import com.truecommunity.session.RecentSearches;
import com.truecommunity.session.RecentlyViewedStatements;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * MVC controller for the public discovery feed and its public-feed SSE stream.
 */
@Controller
public class FeedController {

    private final FeedService feedService;

    private final UserService userService;

    private final SseEventService sseEventService;

    private final RecentSearches recentSearches;

    private final RecentlyViewedStatements recentlyViewed;

    public FeedController(FeedService feedService, UserService userService, SseEventService sseEventService,
                          RecentSearches recentSearches, RecentlyViewedStatements recentlyViewed) {
        this.feedService = feedService;
        this.userService = userService;
        this.sseEventService = sseEventService;
        this.recentSearches = recentSearches;
        this.recentlyViewed = recentlyViewed;
    }

    /**
     * Renders the public feed buckets and stores non-blank keyword searches in
     * the per-session recent-search list.
     *
     * @param searchForm search and filter values
     * @param model MVC model
     * @param authentication current authentication, possibly anonymous
     * @return feed template
     */
    @GetMapping({"/", "/feed"})
    public String feed(@ModelAttribute("searchForm") StatementSearchForm searchForm,
                       Model model,
                       Authentication authentication) {
        User currentUser = currentUserOrNull(authentication);
        FeedView feed = feedService.buildFeed(currentUser, searchForm);

        // Remember non-blank keyword searches for quick re-search chips.
        if (searchForm.getKeyword() != null && !searchForm.getKeyword().isBlank()) {
            recentSearches.add(searchForm.getKeyword().trim());
        }

        model.addAttribute("recentSearches", recentSearches.getItems());
        model.addAttribute("recentlyViewed", recentlyViewed.getItems());
        model.addAttribute("availableItems", feed.getAvailableItems());
        model.addAttribute("availableVoteCounts", feed.getAvailableVoteCounts());
        model.addAttribute("availableCommentCounts", feed.getAvailableCommentCounts());
        model.addAttribute("pendingItems", feed.getPendingItems());
        model.addAttribute("pendingConsensus", feed.getPendingConsensus());
        model.addAttribute("resolvedItems", feed.getResolvedItems());
        model.addAttribute("allTopics", feed.getAllTopics());
        model.addAttribute("verdicts", Verdict.values());
        return "feed";
    }

    /**
     * Opens the public feed SSE stream on channel {@code public-feed}.
     *
     * @return SSE emitter subscribed to public feed events
     */
    @GetMapping(value = "/feed/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter feedEvents() {
        return sseEventService.subscribe(SseChannels.PUBLIC_FEED);
    }

    private User currentUserOrNull(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
