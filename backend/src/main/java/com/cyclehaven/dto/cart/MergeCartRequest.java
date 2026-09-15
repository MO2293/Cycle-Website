package com.cyclehaven.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Hands a guest's browser-held cart to the server at login.
 *
 * <p>This is what replaces the original's shared guest cart. That version stored
 * anonymous carts under the literal email {@code guest@cyclehaven.com}, so every
 * unauthenticated visitor on the site read and wrote the *same* database rows —
 * one guest's cart contents appeared in another's, and checking out as a guest
 * cleared it for everyone. Guests now keep their cart in their own browser and
 * POST it here once they sign in.
 */
public record MergeCartRequest(
        @NotNull(message = "lines is required")
        @Size(max = 50, message = "Cannot merge more than 50 lines at once")
        @Valid
        List<AddToCartRequest> lines) {
}
