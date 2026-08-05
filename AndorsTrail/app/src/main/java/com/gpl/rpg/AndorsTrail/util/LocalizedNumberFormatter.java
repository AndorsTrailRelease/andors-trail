package com.gpl.rpg.AndorsTrail.util;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats brace-wrapped numeric literals using the active locale.
 */
public final class LocalizedNumberFormatter {
    private static final Pattern CURLY_NUMBERS = Pattern.compile("\\{\\s*(-?\\d*\\.?\\d+)\\s*\\}");

    private LocalizedNumberFormatter() { /* no instantiation */ }

    /**
     * Replace brace-wrapped numeric literals with locale-formatted output.
     *
     * @param input input text containing brace-wrapped numbers
     * @param locale target locale, or the default locale when null
     * @return text with localized number formatting
     */
    public static String parseString(String input, Locale locale) {
        if (input == null) return null;
        locale = (locale == null) ? Locale.getDefault() : locale;

        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        Matcher m = CURLY_NUMBERS.matcher(input);
        StringBuilder out = new StringBuilder(input.length() + 16);
        int last = 0;

        while (m.find()) {
            out.append(input, last, m.start());
            final String digits = m.group(1);
            if (digits == null) {
                out.append(m.group(0));
                last = m.end();
                continue;
            }
            out.append(localizeDigits(formatDigits(digits, nf), locale));
            last = m.end();
        }
        out.append(input, last, input.length());
        return out.toString();
    }

    /**
     * Format a parsed numeric token with locale-aware separators.
     *
     * @param digits numeric text to format
     * @param nf number formatter for the target locale
     * @return localized numeric text, or the original token if parsing fails
     */
    private static String formatDigits(String digits, NumberFormat nf) {
        try {
            if (digits.indexOf('.') >= 0) {
                return nf.format(new BigDecimal(digits));
            }
            long value = Long.parseLong(digits);
            return nf.format(value);
        } catch (NumberFormatException ex) {
            try {
                if (digits.indexOf('.') >= 0) {
                    return nf.format(new BigDecimal(digits));
                }
                return nf.format(new BigInteger(digits));
            } catch (Exception ignored) {
                return digits;
            }
        }
    }

    /**
     * Convert ASCII digits in a string to the locale's digit shapes.
     *
     * @param input text to convert
     * @param locale target locale
     * @return text with localized digits
     */
    static String localizeDigits(String input, Locale locale) {
        if (input == null || input.isEmpty()) return input;
        char zeroDigit = DecimalFormatSymbols.getInstance(locale).getZeroDigit();
        if (zeroDigit == '0') return input;

        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch >= '0' && ch <= '9') {
                out.append((char) (zeroDigit + (ch - '0')));
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
