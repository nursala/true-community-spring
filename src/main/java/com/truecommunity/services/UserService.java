package com.truecommunity.services;

import com.truecommunity.dto.AdminUsersView;
import com.truecommunity.dto.RegisterForm;
import com.truecommunity.dto.UserDemotionResult;
import com.truecommunity.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Defines account registration, user lookup, administration, and leaderboard
 * ranking operations.
 */
public interface UserService {

    /**
     * Registers a regular user after validating password confirmation and uniqueness.
     *
     * @param form the registration form
     * @return the persisted user
     */
    User register(RegisterForm form);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    List<User> getAllUsers();

    List<User> searchUsers(String query);

    AdminUsersView getAdminUsersView(String query);

    /**
     * Promotes a user to editor.
     *
     * @param userId the user to promote
     * @return the saved user
     */
    User promoteToEditor(Long userId);

    /**
     * Demotes an editor to a regular user while enforcing administrative safety rules.
     *
     * @param userId the user to demote
     * @param currentEditor the editor performing the action
     * @return demotion details including released statement ids
     */
    UserDemotionResult demoteToUser(Long userId, User currentEditor);

    List<User> getLeaderboard();

    int getRank(User user);
}
