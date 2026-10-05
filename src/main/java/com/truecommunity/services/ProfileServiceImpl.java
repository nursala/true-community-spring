package com.truecommunity.services;

import com.truecommunity.exception.PasswordMismatchException;
import com.truecommunity.exception.UsernameAlreadyExistsException;
import com.truecommunity.model.User;
import com.truecommunity.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional account self-service implementation for username and password
 * changes. It keeps password verification and encoding in the service layer.
 */
@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 50;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public ProfileServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void updateUsername(User user, String newUsername) {
        String trimmed = newUsername == null ? "" : newUsername.trim();
        if (trimmed.length() < USERNAME_MIN || trimmed.length() > USERNAME_MAX) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters.");
        }
        // Allow keeping the same name; reject only if another user already owns it.
        if (!trimmed.equals(user.getUsername()) && userRepository.existsByUsername(trimmed)) {
            throw new UsernameAlreadyExistsException("Username is already taken.");
        }
        user.setUsername(trimmed);
        userRepository.save(user);
    }

    @Override
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new PasswordMismatchException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
