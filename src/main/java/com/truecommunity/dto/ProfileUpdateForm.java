package com.truecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Backs the dashboard "edit username" form. */
public class ProfileUpdateForm {

    @NotBlank(message = "יש להזין שם משתמש.")
    @Size(min = 3, max = 50, message = "שם המשתמש חייב להיות באורך 3 עד 50 תווים.")
    private String username;

    public ProfileUpdateForm() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
