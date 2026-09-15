package com.cyclehaven.controller;

import com.cyclehaven.dto.PageResponse;
import com.cyclehaven.dto.user.AdminUpdateUserRequest;
import com.cyclehaven.dto.user.UserResponse;
import com.cyclehaven.security.CustomUserDetails;
import com.cyclehaven.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin user management — the replacement for {@code adminUsers.jsp}.
 *
 * <p>The original's version had no access check of any kind: the page was a file
 * under the webapp root, and {@code adminUserUpdate} was a servlet anyone could
 * POST to. Everything here sits under {@code /api/admin/**}, which
 * {@code SecurityConfig} restricts to the ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin — Users", description = "User management (admin only)")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List all users")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(userService.listUsers(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one user")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user's details or role")
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(
                userService.adminUpdateUser(id, request, principal.getId()));
    }
}
