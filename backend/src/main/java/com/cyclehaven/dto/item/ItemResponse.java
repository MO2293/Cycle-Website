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
                imageUrl(item));
    }

    /**
     * Builds the image URL with a cache-busting version stamp.
     *
     * <p>Image responses carry a 7-day cache header, which is what you want — a
     * product photo rarely changes, and re-downloading every thumbnail on each
     * page view is pure waste. But it also means a browser that has already
     * cached {@code /api/items/5/image} keeps serving the old picture for a week
     * after an admin replaces it, without ever asking the server.
     *
     * <p>Appending the row's {@code updatedAt} makes the URL itself change
     * whenever the item changes, so the browser treats it as a different
     * resource and fetches it. That gives a long cache lifetime and immediate
     * updates, which would otherwise be in direct conflict.
     */
    private static String imageUrl(Item item) {
        long version = item.getUpdatedAt() != null ? item.getUpdatedAt().toEpochMilli() : 0L;
        return "/api/items/" + item.getId() + "/image?v=" + version;
    }
}
