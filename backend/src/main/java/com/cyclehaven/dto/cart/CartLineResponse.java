package com.cyclehaven.dto.cart;

import com.cyclehaven.entity.CartItem;
import java.math.BigDecimal;

/**
 * One line of the cart, with everything a cart page needs to render a row.
 *
 * <p>{@code lineTotal} is computed on the server. The original passed a
 * client-supplied {@code totalAmount} parameter through to checkout, which means
 * the browser decided what the order cost.
 */
public record CartLineResponse(
        Long itemId,
        String name,
        String model,
        String colour,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        int availableStock,
        String imageUrl) {

    public static CartLineResponse from(CartItem cartItem) {
        BigDecimal unitPrice = cartItem.getItem().getPrice();
        return new CartLineResponse(
                cartItem.getItem().getId(),
                cartItem.getItem().getName(),
                cartItem.getItem().getModel(),
                cartItem.getItem().getColour(),
                unitPrice,
                cartItem.getQuantity(),
                unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())),
                cartItem.getItem().getQuantity(),
                "/api/items/" + cartItem.getItem().getId() + "/image");
    }
}
