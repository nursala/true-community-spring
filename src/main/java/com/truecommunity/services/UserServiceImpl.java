package com.truecommunity.services;

import com.truecommunity.dto.AdminUsersView;
import com.truecommunity.dto.RegisterForm;
import com.truecommunity.dto.UserDemotionResult;
import com.truecommunity.exception.CannotDemoteLastEditorException;
import com.truecommunity.exception.CannotDemoteProtectedUserException;
import com.truecommunity.exception.CannotDemoteSelfException;
import com.truecommunity.exception.EmailAlreadyExistsException;
import com.truecommunity.exception.PasswordMismatchException;
import com.truecommunity.exception.UsernameAlreadyExistsException;
import com.truecommunity.exception.UserNotFoundException;
import com.truecommunity.model.Role;
import com.truecommunity.model.Statement;
import com.truecommunity.model.StatementStatus;
import com.truecommunity.model.User;
import com.truecommunity.repo.StatementRepository;
import com.truecommunity.repo.UserRepository;
import com.truecommunity.security.SessionExpiryService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Transactional user-management implementation. It handles registration,
 * role changes, ranking queries, and session expiration when authorities
 * change. Demotion also releases any under-review statements claimed by the
 * target editor before the role is changed.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final SessionExpiryService sessionExpiryService;

    private final StatementRepository statementRepository;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           SessionExpiryService sessionExpiryService,
                           StatementRepository statementRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionExpiryService = sessionExpiryService;
        this.statementRepository = statementRepository;
    }

    /**
     * Creates a regular user with an encoded password after confirming the
     * submitted passwords match and the email/username are unique.
     *
     * @param form the registration form
     * @return the saved user
     */
    @Override
    public User register(RegisterForm form) {
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            throw new PasswordMismatchException("Password and confirm password do not match.");
        }

        validateUniqueUser(form.getEmail(), form.getUsername());

        User user = User.createRegularUser(
                form.getUsername(),
                form.getEmail(),
                passwordEncoder.encode(form.getPassword())
        );

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchUsers(String query) {
        List<User> all = userRepository.findAllByOrderByUsernameAsc();
        if (query == null || query.isBlank()) {
            return all;
        }
        String needle = query.trim().toLowerCase();
        return all.stream()
                .filter(user -> user.getUsername().toLowerCase().contains(needle)
                        || user.getEmail().toLowerCase().contains(needle))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUsersView getAdminUsersView(String query) {
        if (query == null || query.isBlank()) {
            // Default view: every editor, plus only the top 10 users by trust score.
            List<User> editors = userRepository.findByRoleOrderByUsernameAsc(Role.EDITOR);
            List<User> topUsers = userRepository.findTop10ByRoleNotOrderByTrustScoreDesc(Role.EDITOR);
            return new AdminUsersView(editors, topUsers, true);
        }

        // Active search: match across all users, no cap, split by role.
        List<User> matches = searchUsers(query);
        List<User> editors = matches.stream()
                .filter(user -> user.getRole() == Role.EDITOR)
                .toList();
        List<User> users = matches.stream()
                .filter(user -> user.getRole() != Role.EDITOR)
                .sorted(Comparator.comparingDouble(
                        (User user) -> user.getTrustScore() == null ? 0.0 : user.getTrustScore()).reversed())
                .toList();
        return new AdminUsersView(editors, users, false);
    }

    /**
     * Promotes the user to editor and expires their active sessions only when
     * the role actually changed, forcing Spring Security to reload authorities.
     *
     * @param userId the user to promote
     * @return the saved user
     */
    @Override
    @Transactional
    public User promoteToEditor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        // Idempotent: promoting an existing editor is a no-op beyond re-asserting state.
        boolean roleChanged = user.getRole() != Role.EDITOR;
        user.promoteToEditor();
        User saved = userRepository.save(user);
        // Force a re-login only when authorities actually changed, so a no-op
        // self-promote does not disrupt the acting admin's own session.
        if (roleChanged) {
            sessionExpiryService.expireUserSessions(saved.getEmail());
        }
        return saved;
    }

    /**
     * Demotes an editor transactionally after blocking self-demotion, protected
     * seeded-admin demotion, and removal of the last editor. Claimed
     * under-review statements are released before the role changes, and active
     * sessions are expired when the demotion is real.
     *
     * @param userId the user to demote
     * @param currentEditor the editor performing the action
     * @return demotion result containing the target id, role-change flag, and released statements
     */
    @Override
    @Transactional
    public UserDemotionResult demoteToUser(Long userId, User currentEditor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (currentEditor != null && currentEditor.getId().equals(user.getId())) {
            throw new CannotDemoteSelfException("An editor cannot demote themselves.");
        }

        if (user.isSeededAdmin()) {
            throw new CannotDemoteProtectedUserException("The protected primary editor cannot be demoted.");
        }

        if (user.getRole() == Role.EDITOR && userRepository.countByRole(Role.EDITOR) <= 1) {
            throw new CannotDemoteLastEditorException("Cannot demote the last remaining editor.");
        }

        boolean roleChanged = user.getRole() == Role.EDITOR;
        List<Statement> releasedStatements = roleChanged
                ? statementRepository.findByClaimedByAndStatus(user, StatementStatus.UNDER_REVIEW)
                : List.of();
        List<Long> releasedStatementIds = releasedStatements.stream()
                .map(Statement::getId)
                .toList();
        releasedStatements.forEach(Statement::release);

        user.demoteToUser();
        userRepository.save(user);
        // The target is never the acting admin here (self-demote is blocked), so
        // expiring their sessions makes the lost EDITOR authority take effect now.
        if (roleChanged) {
            sessionExpiryService.expireUserSessions(user.getEmail());
        }
        return new UserDemotionResult(user.getId(), roleChanged, releasedStatementIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getLeaderboard() {
        return userRepository.findTop10ByRoleNotOrderByTrustScoreDesc(Role.EDITOR);
    }

    @Override
    @Transactional(readOnly = true)
    public int getRank(User user) {
        double score = user.getTrustScore() == null ? 0.0 : user.getTrustScore();
        return (int) (userRepository.countByRoleNotAndTrustScoreGreaterThan(Role.EDITOR, score) + 1);
    }

    private void validateUniqueUser(String email, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already registered.");
        }

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException("Username is already registered.");
        }
    }
}
