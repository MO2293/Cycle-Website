package com.cyclehaven.controller;

import com.cyclehaven.dto.auth.AuthResponse;
import com.cyclehaven.dto.auth.LoginRequest;
import com.cyclehaven.dto.auth.RegisterRequest;
import com.cyclehaven.dto.user.UserResponse;
import com.cyclehaven.security.CustomUserDetails;
import com.cyclehaven.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 *
 * <p>Compare with the original {@code LoginServlet}, which parsed parameters,
 * checked a hardcoded backdoor, queried the database, wrote eight attributes into
 * an HttpSession and chose a JSP to redirect to — all in one method. This
 * controller only maps HTTP to a service call and back. That separation is what
 * makes the logic testable without a servlet container.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login, and current-user lookup")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Create a customer account and return a token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 201 Created, not 200: a new resource now exists. The original redirected
        // to login.jsp, so the client had no way to tell success from failure
        // except by looking at which page it landed on.
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange email and password for a token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Return the signed-in user, identified by their token")
    public ResponseEntity<UserResponse> currentUser(
            @AuthenticationPrincipal CustomUserDetails principal) {
        // The id comes from the verified token, never from a query parameter —
        // otherwise any caller could read any account by changing the number.
        return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
    }
}
