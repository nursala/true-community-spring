package com.truecommunity.security;

import com.truecommunity.model.User;
import com.truecommunity.repo.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads login principals by email and maps the stored application role to the
 * Spring Security authority format {@code ROLE_<role>}.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the user used by form login.
     *
     * @param email the submitted email address
     * @return a Spring Security user with the encoded password and role authority
     * @throws UsernameNotFoundException when the email is not registered
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(toAuthority(user))
                .build();
    }

    private String toAuthority(User user) {
        return "ROLE_" + user.getRole().name();
    }
}
