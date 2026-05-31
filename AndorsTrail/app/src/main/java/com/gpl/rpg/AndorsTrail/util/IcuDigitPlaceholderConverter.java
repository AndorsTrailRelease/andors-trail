package com.gpl.rpg.AndorsTrail.util;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IcuDigitPlaceholderConverter {
    // Matches exactly {digits} where digits is one or more ASCII digits.
    // Adjust quantifier if you want min length (e.g. \d{4,}).
    private static final Pattern CURLY_DIGITS = Pattern.compile("\\{\\s*(\\d+)\\s*\\}");
    // Prefix used for generated ICU placeholder names. Keep conservative chars (letters/digits/_)
    // We choose a prefix unlikely to collide with existing identifiers in translation files.
    private static final String PLACEHOLDER_PREFIX = "lt_num_"; // e.g. lt_num_1234

    private IcuDigitPlaceholderConverter() { /* no instantiation */ }

    private static String escapeForMessageFormat(String s) {
        if (s == null || s.isEmpty()) return s;
        // Escape single quote for MessageFormat
        s = s.replace("'", "''");
        // Quote literal braces so MessageFormat doesn't interpret them
        s = s.replace("{", "'{'");
        s = s.replace("}", "'}'");
        return s;
    }

    /**
     * Convert occurrences of "{1234}" into ICU-style named placeholders "{lt_num_1234,number}"
     * and format using android.icu if available, otherwise fallback to a manual NumberFormat
     * replacement which preserves locale-specific grouping/decimal separators.
     *
     * @param input The input string possibly containing {1234}-style placeholders
     * @param locale Target locale (if null uses Locale.getDefault())
     * @return formatted string with locale-aware number formatting
     */
    public static String formatDigitsAsIcu(String input, Locale locale) {
        if (input == null) return null;
        locale = (locale == null) ? Locale.getDefault() : locale;

        // Build a MessageFormat pattern (escaped) and collect values. We also keep a mapping
        // digitText -> placeholderName so we can do a simple manual fallback replacement on the
        // original input when ICU is not available.
        StringBuilder patternBuilder = new StringBuilder(input.length() + 32);
        Matcher m = CURLY_DIGITS.matcher(input);
        int last = 0;
        // Map placeholderName -> numeric object (Long or BigInteger)
        Map<String, Object> namedValues = new LinkedHashMap<>();
        // Map from the literal digit text to the generated placeholder name
        Map<String, String> digitToName = new HashMap<>();

        while (m.find()) {
            int s = m.start();
            int e = m.end();
            // append literal part escaped for MessageFormat (quote single quotes/braces)
            patternBuilder.append(escapeForMessageFormat(input.substring(last, s)));

            String digits = m.group(1);
            // parse number; try Long then BigInteger
            Object numberValue;
            try {
                numberValue = Long.parseLong(digits);
            } catch (NumberFormatException ex) {
                numberValue = new BigInteger(digits);
            }

            // Determine a stable, collision-free placeholder name for this digit text.
            // Reuse the same generated name for identical digit strings (deterministic).
            String name;
            if (digitToName.containsKey(digits)) {
                name = digitToName.get(digits);
            } else {
                String baseName = PLACEHOLDER_PREFIX + digits;
                name = baseName;
                int suffix = 0;
                // Avoid colliding with any existing placeholder name already in the map
                while (namedValues.containsKey(name)) {
                    suffix++;
                    name = baseName + "_" + suffix;
                }
                digitToName.put(digits, name);
                namedValues.put(name, numberValue);
            }

            // Insert ICU number placeholder (use number style). For plural-based replacement you can
            // inject a plural pattern instead of a simple number placeholder when desired.
            patternBuilder.append("{").append(name).append(",number}");

            last = e;
        }
        // append remainder (escaped)
        patternBuilder.append(escapeForMessageFormat(input.substring(last)));
        String pattern = patternBuilder.toString();

        // Try android.icu.text.MessageFormat (Map-based)
        try {
            android.icu.util.ULocale uLocale = android.icu.util.ULocale.forLocale(locale);
            android.icu.text.MessageFormat mf = new android.icu.text.MessageFormat(pattern, uLocale);
            return mf.format(namedValues);
        } catch (Throwable t) {
            // Fallback to a manual replacement using NumberFormat on the original input.
            // We purposely avoid MessageFormat here because java.text.MessageFormat lacks
            // Map-based named arguments and ICU plural/select.
            NumberFormat nf = NumberFormat.getInstance(locale);
            StringBuilder out = new StringBuilder(input.length() + 32);
            Matcher m2 = CURLY_DIGITS.matcher(input);
            int last2 = 0;
            while (m2.find()) {
                out.append(input, last2, m2.start());
                String digits = m2.group(1);
                String name = digitToName.get(digits);
                Object val = (name != null) ? namedValues.get(name) : null;
                String formatted;
                if (val instanceof BigInteger) {
                    // NumberFormat doesn't accept BigInteger directly in all implementations; use toString
                    formatted = nf.format(new java.math.BigDecimal((BigInteger) val));
                } else if (val instanceof Number) {
                    formatted = nf.format((Number) val);
                } else {
                    // Fallback: format the raw digits string (shouldn't normally happen)
                    formatted = digits;
                }
                out.append(formatted);
                last2 = m2.end();
            }
            out.append(input.substring(last2));
            return out.toString();
        }
    }
}

