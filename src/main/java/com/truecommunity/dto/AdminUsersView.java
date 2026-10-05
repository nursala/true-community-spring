package com.truecommunity.dto;

import com.truecommunity.model.User;

import java.util.List;

/** Read-model for the admin users page, assembled by the service layer. */
public class AdminUsersView {

    private final List<User> editors;
    private final List<User> users;
    // True when the users list is the bounded default (top-N) view rather than a full search result.
    private final boolean usersCapped;

    public AdminUsersView(List<User> editors, List<User> users, boolean usersCapped) {
        this.editors = editors;
        this.users = users;
        this.usersCapped = usersCapped;
    }

    public List<User> getEditors() {
        return editors;
    }

    public List<User> getUsers() {
        return users;
    }

    public boolean isUsersCapped() {
        return usersCapped;
    }
}
