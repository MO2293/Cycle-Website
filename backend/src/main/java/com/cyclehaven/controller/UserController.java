package com.cyclehaven.controller;

import com.cyclehaven.dto.user.ChangePasswordRequest;
import com.cyclehaven.dto.user.UpdateProfileRequest;
import com.cyclehaven.dto.user.UserResponse;
import com.cyclehaven.security.CustomUserDetails;
import com.cyclehaven.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profile self-service.
 *
 * <p>Every route is {@code /me} — the user is identified by their token, so there
 * is no id in any path. That is what makes it impossible to read or edit someone
 * else's profile: the API offers no way to name a different user.
 */
@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Profile", description = "The signed-in user's own account")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get your profile")
    public ResponseEntity<UserResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getId()));
    }

    @PutMapping
    @Operation(summary = "Update your profile details")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @PutMapping("/password")
    @Operation(summary = "Change your password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        // 204: it worked, and there is nothing meaningful to return. Echoing the
        // user back here would serve no purpose.
        return ResponseEntity.noContent().build();
    }
}
