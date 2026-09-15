package com.cyclehaven.dto.order;

import com.cyclehaven.entity.Order;
import com.cyclehaven.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A placed order.
 *
 * <p>{@code cardLast4} and {@code cardBrand} are all that remains of the payment
 * details — enough to show "Visa ending in 4242", and nothing more.
 */
public record OrderResponse(
        String orderRef,
        OrderStatus status,
        String customerEmail,
        String shippingAddress,
        BigDecimal totalAmount,
        String cardBrand,
        String cardLast4,
        Instant placedAt,
        List<OrderLineResponse> lines) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderRef(),
                order.getStatus(),
                order.getContactEmail(),
                order.getShippingAddress(),
                order.getTotalAmount(),
                order.getCardBrand(),
                order.getCardLast4(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderLineResponse::from).toList());
    }
}
