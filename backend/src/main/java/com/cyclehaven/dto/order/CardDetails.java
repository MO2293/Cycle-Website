package com.cyclehaven.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Card details supplied at checkout.
 *
 * <p>These values are validated, used to produce a masked last-4, and then
 * discarded. They are never written to the database and never logged.
 *
 * <p>{@code WRITE_ONLY} means Jackson will read these fields from an incoming
 * request but refuses to serialise them into any response — so a card number
 * cannot leak back out through an endpoint that happens to echo its input.
 */
public record CardDetails(

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "^[0-9 -]{12,25}$", message = "Card number may contain only digits, spaces or dashes")
        String number,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "CVV is required")
        @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3 or 4 digits")
        String cvv,

        @NotNull(message = "Expiry month is required")
        @Min(value = 1, message = "Expiry month must be between 1 and 12")
        @Max(value = 12, message = "Expiry month must be between 1 and 12")
        Integer expiryMonth,

        @NotNull(message = "Expiry year is required")
        @Min(value = 2024, message = "Expiry year looks invalid")
        @Max(value = 2099, message = "Expiry year looks invalid")
        Integer expiryYear) {
}
