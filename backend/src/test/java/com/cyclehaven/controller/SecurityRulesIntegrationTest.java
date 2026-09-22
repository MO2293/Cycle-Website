package com.cyclehaven.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cyclehaven.dto.auth.LoginRequest;
import com.cyclehaven.dto.auth.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end checks that the access rules in {@code SecurityConfig} actually
 * hold, run against a real Spring context and an in-memory database.
 *
 * <p>Unit tests verify logic; these verify wiring. A rule that is written
 * correctly but never applied — because a path pattern does not match, or a
 * filter sits in the wrong position — would pass every unit test and still leave
 * the admin area open. This is the layer that catches that, and it is precisely
 * the class of mistake the original had everywhere.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRulesIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    /** Registers a customer and returns their token. */
    private String registerCustomer(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Test Customer", email, "Password123",
                null, null, null, null, null);

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("token").asText();
    }

    @Test
    @DisplayName("Browsing the catalogue needs no account")
    void catalogueIsPublic() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("The cart requires authentication")
    void cartRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                // Must be a JSON body, not an empty response — a JSON client
                // parsing an empty body fails in a way that looks like a network
                // error rather than "log in".
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Creating a product requires authentication")
    void creatingProductRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A signed-in customer is refused access to the admin area")
    void customerCannotReachAdminArea() throws Exception {
        String token = registerCustomer("customer-admin-check@example.com");

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("A tampered token is rejected")
    void tamperedTokenIsRejected() throws Exception {
        String token = registerCustomer("tamper-check@example.com");
        // Flip the final character of the signature.
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Register then read your own profile with the returned token")
    void registerAndReadOwnProfile() throws Exception {
        String token = registerCustomer("profile-check@example.com");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile-check@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                // The hash must never appear in any response.
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("A weak password is rejected with field-level detail")
    void rejectsWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Weak Password", "weak@example.com", "short",
                null, null, null, null, null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("Registering cannot grant yourself the ADMIN role")
    void cannotSelfAssignAdminRole() throws Exception {
        // RegisterRequest has no role field, so an injected one is ignored
        // rather than honoured. This is what prevents privilege escalation.
        String payload = """
                {
                  "name": "Sneaky",
                  "email": "sneaky@example.com",
                  "password": "Password123",
                  "role": "ADMIN"
                }
                """;

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode user = objectMapper.readTree(body).get("user");
        assertThat(user.get("role").asText()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Login with the wrong password returns 401")
    void loginWithWrongPasswordFails() throws Exception {
        registerCustomer("login-check@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("login-check@example.com", "WrongPassword1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
