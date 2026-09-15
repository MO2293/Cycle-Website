package com.cyclehaven.dto.user;

import com.cyclehaven.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What an admin may change about another user.
 *
 * <p>Same fields as a self-service profile update, plus {@code role} — granting
 * or revoking admin rights is the one thing only an admin can do.
 *
 * <p>Still no password field. An admin should not be able to set a user's
 * password to a value they know; that would let them sign in as that user and
 * act as them indistinguishably. Password resets belong in an email-based flow.
 */
public record AdminUpdateUserRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @Size(max = 30)
        String phone,

        @Size(max = 500)
        String address,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String province,

        @Size(max = 100)
        String country,

        @NotNull(message = "Role is required")
        Role role) {
}
