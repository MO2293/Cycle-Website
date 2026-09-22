package com.cyclehaven.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cyclehaven.dto.auth.AuthResponse;
import com.cyclehaven.dto.auth.LoginRequest;
import com.cyclehaven.dto.auth.RegisterRequest;
import com.cyclehaven.entity.Role;
import com.cyclehaven.entity.User;
import com.cyclehaven.exception.ConflictException;
import com.cyclehaven.repository.UserRepository;
import com.cyclehaven.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;

    // A real encoder rather than a mock: the point of these tests is that
    // hashing genuinely happens, which a stubbed encoder would hide.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService() {
        return new AuthService(userRepository, passwordEncoder, jwtService);
    }

    private RegisterRequest registration() {
        return new RegisterRequest(
                "Test Customer", "Customer@Example.com", "Password123",
                "416-555-0134", "123 Bloor St W", "Toronto", "ON", "Canada");
    }

    @Test
    @DisplayName("Registration stores a BCrypt hash, never the plaintext password")
    void storesHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);

        authService().register(registration());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        String stored = saved.getValue().getPasswordHash();

        assertThat(stored).isNotEqualTo("Password123");
        assertThat(stored).startsWith("$2a$");
        // The stored value must still verify against the original password.
        assertThat(passwordEncoder.matches("Password123", stored)).isTrue();
    }

    @Test
    @DisplayName("New accounts are always CUSTOMER, never ADMIN")
    void newAccountsAreCustomers() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);

        AuthResponse response = authService().register(registration());

        assertThat(response.user().role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Email is normalised to lower case so addresses cannot be duplicated by case")
    void normalisesEmail() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);

        authService().register(registration());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("customer@example.com");
    }

    @Test
    @DisplayName("Registering an email that already exists is a conflict")
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService().register(registration()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("A wrong password is rejected")
    void rejectsWrongPassword() {
        User existing = new User();
        existing.setId(1L);
        existing.setEmail("customer@example.com");
        existing.setPasswordHash(passwordEncoder.encode("Password123"));
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService()
                .login(new LoginRequest("customer@example.com", "WrongPassword1")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("An unknown email fails with the same message as a wrong password")
    void unknownEmailIsIndistinguishableFromWrongPassword() {
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());

        // Identical messages prevent using login responses to discover which
        // email addresses have accounts.
        assertThatThrownBy(() -> authService()
                .login(new LoginRequest("nobody@example.com", "Password123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("A correct password returns a token")
    void successfulLoginReturnsToken() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Test Customer");
        existing.setEmail("customer@example.com");
        existing.setPasswordHash(passwordEncoder.encode("Password123"));
        existing.setRole(Role.CUSTOMER);

        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("a.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);

        AuthResponse response =
                authService().login(new LoginRequest("customer@example.com", "Password123"));

        assertThat(response.token()).isEqualTo("a.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("customer@example.com");
    }
}
