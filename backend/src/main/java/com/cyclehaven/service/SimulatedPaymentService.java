package com.cyclehaven.service;

import com.cyclehaven.dto.order.CardDetails;
import com.cyclehaven.util.CardUtils;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * A stand-in for a real payment processor.
 *
 * <p><b>No money moves and no card is ever charged.</b> This project deliberately
 * integrates no payment provider; the class exists so checkout has a realistic
 * shape without pretending to be something it isn't.
 *
 * <p>What it does do is apply the checks a real gateway would apply before
 * sending anything for authorisation — Luhn and expiry — so invalid input is
 * rejected the same way, and declines can be exercised end to end.
 */
@Service
public class SimulatedPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentService.class);

    /**
     * Any card number ending in these digits is treated as declined, so the
     * failure path is testable without needing a real gateway's sandbox.
     */
    private static final String DECLINE_SUFFIX = "0002";

    @Override
    public PaymentResult authorise(CardDetails card, BigDecimal amount) {
        if (!CardUtils.passesLuhn(card.number())) {
            return PaymentResult.declined("Card number is not valid");
        }
        if (!CardUtils.isExpiryInFuture(card.expiryMonth(), card.expiryYear())) {
            return PaymentResult.declined("Card has expired");
        }
        if (CardUtils.lastFour(card.number()).equals(DECLINE_SUFFIX)) {
            return PaymentResult.declined("Card was declined by the issuer");
        }

        String reference = "SIM-" + UUID.randomUUID();
        // Logs the amount and a reference — never the card number, not even masked
        // beyond what the order itself already stores.
        log.info("Simulated payment approved: amount={} reference={}", amount, reference);
        return PaymentResult.approved(reference);
    }
}
