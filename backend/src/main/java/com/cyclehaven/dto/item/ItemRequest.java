package com.cyclehaven.dto.item;

import com.cyclehaven.entity.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Admin payload for creating or updating a product.
 *
 * <p>The original {@code ItemServlet} read raw parameters and called
 * {@code Double.parseDouble(request.getParameter("price"))} with no checks, so a
 * blank or non-numeric price threw a NumberFormatException that surfaced as a
 * 500. Here the constraints reject bad input before any code runs, and
 * {@code Category} being an enum means a typo like "4124" — which is genuinely in
 * the old production data — can no longer be saved.
 */
public record ItemRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200)
        String name,

        @NotNull(message = "Category is required")
        Category category,

        @NotBlank(message = "Description is required")
        @Size(max = 2000)
        String description,

        @NotBlank(message = "Model is required")
        @Size(max = 120)
        String model,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Price may have at most 2 decimal places")
        BigDecimal price,

        @NotBlank(message = "Colour is required")
        @Size(max = 60)
        String colour,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity) {
}
