package com.cyclehaven.service;

import com.cyclehaven.dto.order.CardDetails;
import java.math.BigDecimal;

/**
 * Takes payment for an order.
 *
 * <p>An interface with one simulated implementation, rather than payment logic
 * inlined into checkout. That keeps the boundary honest: {@code OrderService}
 * asks for an authorisation and gets an outcome, with no idea whether a real
 * processor was involved. Swapping in Stripe later means writing a second
 * implementation, not editing checkout.
 */
public interface PaymentService {

    /**
     * Attempts to authorise a payment.
     *
     * @return the outcome; never throws for an ordinary decline, since a declined
     *         card is a normal result rather than an exceptional one.
     */
    PaymentResult authorise(CardDetails card, BigDecimal amount);

    /**
     * @param reference the processor's reference for a successful authorisation
     */
    record PaymentResult(boolean approved, String reference, String declineReason) {

        public static PaymentResult approved(String reference) {
            return new PaymentResult(true, reference, null);
        }

        public static PaymentResult declined(String reason) {
            return new PaymentResult(false, null, reason);
        }
    }
}
