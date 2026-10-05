package com.truecommunity.security;

import com.truecommunity.exception.UserNotFoundException;
import com.truecommunity.model.User;
import com.truecommunity.repo.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Resolves the current authenticated principal to the latest {@link User}
 * entity from the database, avoiding stale role/profile data from the security
 * session principal.
 */
@Service
public class AuthenticatedUserProvider {

    private final UserRepository userRepository;

    public AuthenticatedUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the authenticated user by the email stored in Spring Security's principal.
     *
     * @param authentication the current authentication
     * @return the current database user
     */
    public User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotFoundException("Authenticated user not found.");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
}
