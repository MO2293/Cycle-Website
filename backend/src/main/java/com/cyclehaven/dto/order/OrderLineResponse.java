package com.cyclehaven.dto.order;

import com.cyclehaven.entity.OrderItem;
import java.math.BigDecimal;

public record OrderLineResponse(
        Long itemId,
        String name,
        String model,
        int quantity,
        BigDecimal priceAtPurchase,
        BigDecimal lineTotal,
        String imageUrl) {

    public static OrderLineResponse from(OrderItem orderItem) {
        return new OrderLineResponse(
                orderItem.getItem().getId(),
                orderItem.getItem().getName(),
                orderItem.getItem().getModel(),
                orderItem.getQuantity(),
                // The price as it was when the order was placed — not the current
                // catalogue price. Reading the live price here is what makes the
                // original's sales report silently rewrite history.
                orderItem.getPriceAtPurchase(),
                orderItem.getLineTotal(),
                "/api/items/" + orderItem.getItem().getId() + "/image");
    }
}
