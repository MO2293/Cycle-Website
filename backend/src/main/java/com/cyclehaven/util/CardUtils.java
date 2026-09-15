package com.cyclehaven.util;

import java.time.YearMonth;

/**
 * Card number checks and masking.
 *
 * <p>Scope matters here: this project does <b>not</b> process payments. There is
 * no processor, no money moves, and checkout is explicitly simulated. These
 * helpers exist to validate input in the way a real checkout would, and — more
 * importantly — to make sure only a masked last-4 ever leaves this class.
 *
 * <p>The original stored the full card number and CVV as plaintext columns in the
 * {@code account} and {@code purchases} tables. Real systems never do this: card
 * data is handed directly to a payment processor, which returns a token, and the
 * merchant stores the token. Nothing in this class ever persists or logs a full
 * number.
 */
public final class CardUtils {

    private CardUtils() {
    }

    /**
     * The Luhn checksum — the same check every real card network uses to catch
     * mistyped numbers before a request is ever sent for authorisation.
     *
     * <p>Working right to left, double every second digit; if doubling takes it
     * above 9, subtract 9. A valid number's digits sum to a multiple of 10.
     */
    public static boolean passesLuhn(String cardNumber) {
        String digits = digitsOnly(cardNumber);
        if (digits.length() < 12 || digits.length() > 19) {
            return false;
        }

        int sum = 0;
        boolean doubleThis = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleThis) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleThis = !doubleThis;
        }
        return sum % 10 == 0;
    }

    public static boolean isExpiryInFuture(int month, int year) {
        if (month < 1 || month > 12) {
            return false;
        }
        // A card is valid through the last day of its expiry month.
        return !YearMonth.of(year, month).isBefore(YearMonth.now());
    }

    /**
     * Identifies the network from the leading digits, for display only
     * ("Visa ending in 4242").
     */
    public static String detectBrand(String cardNumber) {
        String digits = digitsOnly(cardNumber);
        if (digits.startsWith("4")) {
            return "Visa";
        }
        if (digits.matches("^5[1-5].*") || digits.matches("^2[2-7].*")) {
            return "Mastercard";
        }
        if (digits.matches("^3[47].*")) {
            return "American Express";
        }
        if (digits.matches("^6(?:011|5).*")) {
            return "Discover";
        }
        return "Card";
    }

    /** The only part of a card number this application is ever allowed to keep. */
    public static String lastFour(String cardNumber) {
        String digits = digitsOnly(cardNumber);
        return digits.length() < 4 ? digits : digits.substring(digits.length() - 4);
    }

    private static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
