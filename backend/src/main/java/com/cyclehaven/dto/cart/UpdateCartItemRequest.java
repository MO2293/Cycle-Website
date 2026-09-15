package com.cyclehaven.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Sets a cart line to an absolute quantity — what the quantity box on a cart page
 * does.
 *
 * <p>Distinct from {@link AddToCartRequest}, which increments. Conflating "add 2"
 * with "set to 2" is a real source of bugs: press the same button twice and one
 * interpretation gives you 4, the other gives you 2.
 */
public record UpdateCartItemRequest(
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1 — use DELETE to remove the line")
        @Max(value = 99, message = "Quantity may be at most 99 per line")
        Integer quantity) {
}
