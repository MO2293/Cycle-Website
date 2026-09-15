package com.cyclehaven.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Changing your own password.
 *
 * <p>The current password is required even though the caller is already
 * authenticated. A token can be lurking in an unattended browser session; asking
 * for the existing password means a stolen session cannot be used to lock the
 * real owner out of their account.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must contain at least one letter and one number")
        String newPassword) {
}
