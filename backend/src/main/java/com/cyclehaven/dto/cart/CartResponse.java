package com.cyclehaven.dto.cart;

import java.math.BigDecimal;
import java.util.List;

/**
 * The whole cart. Every mutating cart endpoint returns this, so a client never
 * has to guess the new state or issue a follow-up GET after every change.
 *
 * @param notices human-readable messages about adjustments the server made — for
 *                example a quantity reduced because stock ran short. The original
 *                clamped quantities silently, so a customer who asked for 10 got
 *                3 with no explanation.
 */
public record CartResponse(
        List<CartLineResponse> lines,
        int distinctItems,
        int totalUnits,
        BigDecimal subtotal,
        List<String> notices) {
}
