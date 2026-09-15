package com.cyclehaven.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Adds a quantity of a product to the cart, on top of whatever is already there.
 */
public record AddToCartRequest(
        @NotNull(message = "itemId is required")
        Long itemId,

        // At least 1: "add zero of something" is meaningless. The original used
        // quantity=0 as a hidden signal to *remove* an item, which is why its
        // add-to-cart endpoint could silently delete from the cart.
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Quantity may be at most 99 per line")
        Integer quantity) {
}
