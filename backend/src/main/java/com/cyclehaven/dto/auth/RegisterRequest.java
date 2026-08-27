package com.cyclehaven.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Signup payload.
 *
 * <p>A record, not an entity: what a client may send is deliberately a different
 * shape from what is stored. There is no {@code role} field here, so a caller
 * cannot register themselves as an ADMIN by adding one to the JSON — a real risk
 * if the entity were bound directly to the request body.
 *
 * <p>Validation is declarative and runs before any controller code, replacing the
 * original's single {@code password.equals(confirmPassword)} check.
 */
public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must contain at least one letter and one number")
        String password,

        @Size(max = 30)
        String phone,

        @Size(max = 500)
        String address,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String province,

        @Size(max = 100)
        String country) {
}
