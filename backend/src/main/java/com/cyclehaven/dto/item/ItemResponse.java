package com.cyclehaven.dto.item;

import com.cyclehaven.entity.Category;
import com.cyclehaven.entity.Item;
import java.math.BigDecimal;

/**
 * A product as the storefront sees it.
 *
 * <p>{@code imageUrl} is a link rather than embedded image bytes: the browser
 * fetches and caches it separately, so a page of 20 products stays a small JSON
 * response instead of megabytes of base64.
 */
public record ItemResponse(
        Long id,
        String name,
        Category category,
        String description,
        String model,
        BigDecimal price,
        String colour,
        int quantity,
        boolean inStock,
        boolean hasImage,
        String imageUrl) {

    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getDescription(),
                item.getModel(),
                item.getPrice(),
                item.getColour(),
                item.getQuantity(),
                item.isInStock(),
                item.getImageContentType() != null,
                "/api/items/" + item.getId() + "/image");
    }
}
