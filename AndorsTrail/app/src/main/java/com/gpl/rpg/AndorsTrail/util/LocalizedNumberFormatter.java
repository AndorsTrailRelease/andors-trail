package com.gpl.rpg.AndorsTrail.util;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalizedNumberFormatter {
    // Matches literal number blocks like "{1234}", "{12.5}" or "{.5}".
    private static final Pattern CURLY_NUMBERS = Pattern.compile("\\{\\s*(-?\\d*\\.?\\d+)\\s*\\}");

    private LocalizedNumberFormatter() { /* no instantiation */ }

    /**
     * Replace occurrences of "{1234}" with locale-formatted digits.
     *
     * @param input The input string possibly containing {1234}-style placeholders
     * @param locale Target locale (if null uses Locale.getDefault())
     * @return formatted string with locale-aware number formatting
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
            out.append(formatDigits(digits, nf));
            last = m.end();
        }
        out.append(input, last, input.length());
        return out.toString();
    }

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
}
