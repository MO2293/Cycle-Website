package com.cyclehaven.service;

import com.cyclehaven.dto.auth.AuthResponse;
import com.cyclehaven.dto.auth.LoginRequest;
import com.cyclehaven.dto.auth.RegisterRequest;
import com.cyclehaven.dto.user.UserResponse;
import com.cyclehaven.entity.Role;
import com.cyclehaven.entity.User;
import com.cyclehaven.exception.ConflictException;
import com.cyclehaven.exception.NotFoundException;
import com.cyclehaven.repository.UserRepository;
import com.cyclehaven.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login.
 *
 * <p>The two security fixes that matter most in the whole rewrite live here.
 *
 * <p><b>1. No hardcoded admin.</b> The original {@code LoginServlet} began with:
 * <pre>
 *   if ("EECS4413@gmail.com".equalsIgnoreCase(email) &amp;&amp; "4413".equals(password)) {
 *       session.setAttribute("email", email);
 *       response.sendRedirect("adminIndex.jsp");
 *   }
 * </pre>
 * That is a backdoor into full admin, compiled into the application, with the
 * credentials also published in the repository's README. It is gone. Admin now
 * comes from a {@code role} column on a real account.
 *
 * <p><b>2. No plaintext passwords.</b> The original compared credentials with
 * {@code select * from account where email = ? AND password = ?} — meaning every
 * password was stored readable. Anyone with a copy of that database had every
 * customer's password, which matters well beyond this app because people reuse
 * passwords. Now only a BCrypt hash is stored, and login re-hashes the attempt
 * and compares digests.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with that email already exists");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        // The plaintext password exists only inside this method and is never
        // stored, logged, or returned.
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setCity(request.city());
        user.setProvince(request.province());
        user.setCountry(request.country());

        User saved = userRepository.save(user);
        log.info("Registered new user id={}", saved.getId());

        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Identical message whether the email or the password was wrong, so the
            // response cannot be used to discover which emails have accounts.
            throw new BadCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> NotFoundException.of("User", userId));
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getExpirationSeconds(), UserResponse.from(user));
    }
}
