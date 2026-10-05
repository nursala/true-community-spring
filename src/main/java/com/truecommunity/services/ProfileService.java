package com.truecommunity.services;

import com.truecommunity.model.User;

/** Account self-service operations for the authenticated user. */
public interface ProfileService {

    void updateUsername(User user, String newUsername);

    void changePassword(User user, String currentPassword, String newPassword);
}
