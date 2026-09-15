package com.cyclehaven.dto.order;

import com.cyclehaven.dto.cart.AddToCartRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Checkout for a visitor with no account.
 *
 * <p>A guest has no server-side cart, so the lines come from their browser. Only
 * item ids and quantities are accepted — prices are still looked up server-side,
 * so a guest cannot dictate what anything costs either.
 */
public record GuestCheckoutRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Shipping address is required")
        @Size(max = 500)
        String shippingAddress,

        @NotEmpty(message = "At least one item is required")
        @Size(max = 50, message = "An order may contain at most 50 lines")
        @Valid
        List<AddToCartRequest> items,

        @NotNull(message = "Card details are required")
        @Valid
        CardDetails card) {
}
