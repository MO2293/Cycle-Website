package com.cyclehaven.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Fields a user may change about themselves.
 *
 * <p>Deliberately absent: {@code email}, {@code role}, and {@code password}.
 *
 * <p>Email is the login identity, so changing it belongs in its own flow with
 * re-verification. Role is absent so a customer cannot promote themselves by
 * adding {@code "role": "ADMIN"} to the JSON. Password has its own endpoint
 * because changing it requires proving you know the current one.
 *
 * <p>That last separation fixes a real bug. The original's
 * {@code updateProfileServlet} read a {@code password} parameter and passed it to
 * {@code UserDAOImpl.updateUser}, whose SQL was:
 * <pre>
 *   update account set name=?, phone=?, address=?, city=?, province=?, country=?, card=?
 *   where email=?
 * </pre>
 * No password column. So a user could type a new password on the profile page,
 * get a success redirect, and have nothing happen — then find their old password
 * still worked and the new one didn't.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @Size(max = 30)
        @Pattern(regexp = "^$|^[0-9+\\-() ]{7,30}$", message = "Phone number looks invalid")
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
