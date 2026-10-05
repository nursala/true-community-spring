package com.truecommunity.controllers;

import com.truecommunity.services.SseChannels;
import com.truecommunity.services.SseEventService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * MVC controller for the leaderboard shell and its live update stream.
 */
@Controller
public class LeaderboardController {

    private final SseEventService sseEventService;

    public LeaderboardController(SseEventService sseEventService) {
        this.sseEventService = sseEventService;
    }

    /**
     * Renders the leaderboard shell; ranking data is fetched from {@code /api/users/leaderboard}.
     *
     * @return leaderboard template
     */
    @GetMapping("/leaderboard")
    public String leaderboard() {
        return "leaderboard";
    }

    /**
     * Opens the public-feed SSE stream for leaderboard refreshes when decisions
     * change trust scores.
     *
     * @return SSE emitter subscribed to public feed events
     */
    @GetMapping(value = "/leaderboard/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter leaderboardEvents() {
        return sseEventService.subscribe(SseChannels.PUBLIC_FEED);
    }
}
