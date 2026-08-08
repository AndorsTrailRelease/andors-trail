package com.gpl.rpg.AndorsTrail.util;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Centralized formatting helpers for user-facing numbers.
 */
public final class Format {
    private Format() { }

    public static String localizeInt(int value) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(value);
    }

    public static String localizeIntNoSep(int value) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.getDefault());
        nf.setGroupingUsed(false);
        return nf.format(value);
    }

    public static String localizePercentCeil(double floatval) {
        NumberFormat pf = NumberFormat.getPercentInstance(Locale.getDefault());
        pf.setMinimumFractionDigits(0);
        pf.setMaximumFractionDigits(0);
        pf.setRoundingMode(RoundingMode.CEILING);
        return pf.format(floatval);
    }

    public static String localizePercentFromIntPercent(int percent) {
        return localizePercentCeil(percent / 100.0);
    }

    public static String localizeFloat(float value, int minFractionDigits, int maxFractionDigits) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        nf.setMinimumFractionDigits(minFractionDigits);
        nf.setMaximumFractionDigits(maxFractionDigits);
        return nf.format(value);
    }

    public static String localizeFloat(float value, int fractionDigits) {
        return localizeFloat(value, fractionDigits, fractionDigits);
    }

    // Default to 2 fraction digits, which is a common choice for user-facing floats.
    public static String localizeFloat(float value) {
        return localizeFloat(value, 2, 2);
    }

    /** * Replace any contiguous sequence of 4+ digits with a localized integer string, * implemented without using Matcher.appendReplacement/appendTail. */
    public static String localizeLongDigitSequences(String input) {
        if (input == null || input.isEmpty()) return input;

        Pattern p = Pattern.compile("\\d{4,}"); // match 4 or more digits (greedy)
        Matcher m = p.matcher(input);
        StringBuilder out = new StringBuilder(input.length() + 16);
        int lastIndex = 0;
        Locale locale = Locale.getDefault();

        while (m.find()) {
            int start = m.start();
            int end = m.end();
            // append text between last match and this match
            out.append(input.substring(lastIndex, start));

            String digits = m.group();
            String replacement;
            try {
                // try parse as long first (covers most cases and is fast)
                long val = Long.parseLong(digits);
                if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                    // uses the project's existing localized int formatting
                    replacement = Format.localizeInt((int) val);
                } else {
                    NumberFormat nf = NumberFormat.getIntegerInstance(locale);
                    replacement = nf.format(val);
                }
            } catch (NumberFormatException e) {
                // number too large for long -> use BigInteger and NumberFormat if possible
                try {
                    BigInteger bi = new BigInteger(digits);
                    NumberFormat nf = NumberFormat.getIntegerInstance(locale);
                    // NumberFormat.format(Number) supports BigInteger
                    replacement = nf.format(bi);
                } catch (Exception ex) {
                    // parsing failed for some reason; fall back to original digits
                    replacement = digits;
                }
            }

            out.append(replacement);
            lastIndex = end;
        }

        // append the remainder of the input
        if (lastIndex < input.length()) out.append(input.substring(lastIndex));
        return out.toString();
    }
}

