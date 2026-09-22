package com.cyclehaven.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CardUtilsTest {

    @Test
    @DisplayName("Accepts well-known valid test card numbers")
    void acceptsValidCardNumbers() {
        assertThat(CardUtils.passesLuhn("4242424242424242")).isTrue();
        assertThat(CardUtils.passesLuhn("5555555555554444")).isTrue();
        assertThat(CardUtils.passesLuhn("378282246310005")).isTrue();
    }

    @Test
    @DisplayName("Rejects a number with a single mistyped digit")
    void rejectsSingleDigitTypo() {
        // Exactly what Luhn exists to catch: 4242...4242 with the last digit
        // changed. The length and format are still plausible; only the checksum
        // gives it away.
        assertThat(CardUtils.passesLuhn("4242424242424243")).isFalse();
    }

    @Test
    @DisplayName("Rejects numbers that are too short, too long, or non-numeric")
    void rejectsMalformedNumbers() {
        assertThat(CardUtils.passesLuhn("4242")).isFalse();
        assertThat(CardUtils.passesLuhn("42424242424242424242424")).isFalse();
        assertThat(CardUtils.passesLuhn("")).isFalse();
        assertThat(CardUtils.passesLuhn(null)).isFalse();
    }

    @Test
    @DisplayName("Ignores spaces and dashes, as typed on a real checkout form")
    void ignoresFormattingCharacters() {
        assertThat(CardUtils.passesLuhn("4242 4242 4242 4242")).isTrue();
        assertThat(CardUtils.passesLuhn("4242-4242-4242-4242")).isTrue();
    }

    @Test
    @DisplayName("A card is valid through the final day of its expiry month")
    void treatsCurrentMonthAsValid() {
        YearMonth now = YearMonth.now();
        assertThat(CardUtils.isExpiryInFuture(now.getMonthValue(), now.getYear())).isTrue();

        YearMonth lastMonth = now.minusMonths(1);
        assertThat(CardUtils.isExpiryInFuture(lastMonth.getMonthValue(), lastMonth.getYear()))
                .isFalse();
    }

    @Test
    @DisplayName("Rejects impossible month values")
    void rejectsInvalidMonths() {
        assertThat(CardUtils.isExpiryInFuture(0, 2099)).isFalse();
        assertThat(CardUtils.isExpiryInFuture(13, 2099)).isFalse();
    }

    @Test
    @DisplayName("Identifies the card network from its leading digits")
    void detectsBrand() {
        assertThat(CardUtils.detectBrand("4242424242424242")).isEqualTo("Visa");
        assertThat(CardUtils.detectBrand("5555555555554444")).isEqualTo("Mastercard");
        assertThat(CardUtils.detectBrand("378282246310005")).isEqualTo("American Express");
        assertThat(CardUtils.detectBrand("6011111111111117")).isEqualTo("Discover");
        assertThat(CardUtils.detectBrand("9999999999999999")).isEqualTo("Card");
    }

    @Test
    @DisplayName("Returns only the last four digits, never more")
    void returnsOnlyLastFour() {
        // Deliberately a number with no repeating pattern, so "the last four"
        // is unambiguous — 4242424242424242 would make any such assertion
        // meaningless, since every group of four digits is identical.
        String full = "4539578763621486";
        String masked = CardUtils.lastFour(full);

        assertThat(masked).isEqualTo("1486").hasSize(4);
        assertThat(full).endsWith(masked);

        // The guarantee that matters: what gets stored is four digits, not the
        // sixteen it came from.
        assertThat(masked.length()).isEqualTo(4).isLessThan(full.length());
    }

    @Test
    @DisplayName("Masking handles formatted input and short values safely")
    void maskingIsRobust() {
        assertThat(CardUtils.lastFour("4539 5787 6362 1486")).isEqualTo("1486");
        assertThat(CardUtils.lastFour("4539-5787-6362-1486")).isEqualTo("1486");
        // Shorter than four digits: return what there is rather than throwing
        // StringIndexOutOfBounds.
        assertThat(CardUtils.lastFour("12")).isEqualTo("12");
        assertThat(CardUtils.lastFour(null)).isEmpty();
    }
}
