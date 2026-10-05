package com.truecommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Backs the dashboard "change password" form. */
public class PasswordChangeForm {

    @NotBlank(message = "יש להזין את הסיסמה הנוכחית.")
    private String currentPassword;

    @NotBlank(message = "יש להזין סיסמה חדשה.")
    @Size(min = 6, message = "הסיסמה החדשה חייבת להיות באורך 6 תווים לפחות.")
    private String newPassword;

    @NotBlank(message = "יש לאמת את הסיסמה החדשה.")
    private String confirmPassword;

    public PasswordChangeForm() {
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
