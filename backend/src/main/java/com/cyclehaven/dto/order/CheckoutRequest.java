package com.cyclehaven.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Checkout payload for a signed-in customer.
 *
 * <p>Note what is <b>not</b> here: any mention of price, total, or which items are
 * being bought. The items come from the user's server-side cart and the total is
 * calculated from database prices.
 *
 * <p>The original accepted the total from the browser:
 * <pre>
 *   double totalAmount = Double.parseDouble(request.getParameter("totalAmount"));
 * </pre>
 * Anyone could edit that hidden form field and pay whatever they liked. The fix
 * is not to validate the number the client sends — it is to stop accepting one.
 */
public record CheckoutRequest(
        @NotBlank(message = "Shipping address is required")
        @Size(max = 500)
        String shippingAddress,

        @NotNull(message = "Card details are required")
        @Valid
        CardDetails card) {
}
