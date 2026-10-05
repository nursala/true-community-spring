package com.truecommunity.controllers;

import com.truecommunity.dto.LeaderboardResponse;
import com.truecommunity.dto.UserResponse;
import com.truecommunity.model.User;
import com.truecommunity.services.LeaderboardService;
import com.truecommunity.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for public user summaries and leaderboard data.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    private final LeaderboardService leaderboardService;

    public UserController(UserService userService, LeaderboardService leaderboardService) {
        this.userService = userService;
        this.leaderboardService = leaderboardService;
    }

    /**
     * Returns one public user summary by id.
     *
     * @param id user id
     * @return user response or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(this::toUserResponse)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Returns leaderboard entries with optional sorting and scope controls.
     *
     * @param sort requested sort
     * @param scope requested scope
     * @param authentication current authentication, possibly anonymous
     * @return leaderboard response
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String scope,
            Authentication authentication) {
        User currentUser = currentUserOrNull(authentication);
        return ResponseEntity.ok(leaderboardService.build(sort, scope, currentUser));
    }

    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().name());
        response.setTrustScore(user.getTrustScore());
        return response;
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
