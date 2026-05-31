package com.gpl.rpg.AndorsTrail.util;

import android.icu.util.ULocale;
import android.os.Build;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

public final class IcuMessageParser {
    // Matches exactly {digits} where digits is one or more ASCII digits.
    // Adjust quantifier if you want min length (e.g. \\d{4,}).
    private static final Pattern CURLY_DIGITS = Pattern.compile("\\{\\s*(-?\\d)+\\s*\\}");
    private static final String LITERAL_PREFIX = "literal_"; // prefix for literal text placeholders

    private IcuMessageParser() { /* no instantiation */ }

    /**Convert occurrences of "{1234}" into ICU-style named placeholders "{n1234,number}".
     * and format using android.icu if available, otherwise fallback to java.text.MessageFormat.
     * The ICU Message Format supports named placeholders like {name,number} which can be formatted
     * according to locale.  However, it does not support localization of in-line data; it expects
     * placeholders with content supplied at runtime.  This method allows us to treat literal numeric
     * text in the input as if it were a placeholder, so that it can be formatted according to locale rules.
     * (In actual ICU format, this string would be an index-based placeholder, but we explicitly don't
     * support that usage.)
     *
     * @param input The input string possibly containing {1234}-style placeholders
     * @return formatted string with locale-aware number formatting
     **/
    public static String formatCurlyDigits(String input) {
        if (input == null) return null;
        Locale locale = Locale.getDefault();

        // Build replaced pattern and collect values. We reuse the same name for identical numeric text.
        StringBuilder patternBuilder = new StringBuilder(input.length() + 32);
        Matcher m = CURLY_DIGITS.matcher(input);
        int last = 0;
        // Map placeholderName -> numeric object (Long or BigInteger)
        Map<String, Object> namedValues = new LinkedHashMap<>();

        while (m.find()) {
            int s = m.start();
            int e = m.end();
            // append literal part unchanged (do not globally escape; keep original text)
            patternBuilder.append(input, last, s);

            String digits = m.group(1);
            // canonicalize digits if needed (e.g., remove leading zeros) - choose to preserve numeric value
            String name = LITERAL_PREFIX + digits; // safe: begins with letter so it's a named identifier

            // Insert ICU number placeholder
            patternBuilder.append("{").append(name).append(",number}");

            // parse number; try Long then BigInteger
            Object numberValue;
            try {
                numberValue = Long.parseLong(digits);
            } catch (NumberFormatException ex) {
                numberValue = new BigInteger(digits);
            }
            // If same name already exists, we keep first value (same digits -> same number).
            namedValues.put(name, numberValue);

            last = e;
        }
        patternBuilder.append(input.substring(last));
        String pattern = patternBuilder.toString();

        // Try android.icu.text.MessageFormat (Map-based)
        try {
            // Use android.icu if available at runtime (typical on Android API >= 24 or via ICU4J bundled)
            // We reference classes directly; if class unavailable this will throw and fall back.
            android.icu.util.ULocale uLocale = null;
            android.icu.text.MessageFormat mf = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uLocale = ULocale.forLocale(locale);
                mf = new android.icu.text.MessageFormat(pattern, uLocale);
            }
            assert mf != null;
            return mf.format(namedValues); // Will throw exception on older systems, but we catch it and fall back to java.text.MessageFormat.
        } catch (Throwable t) {
            // Fallback to java.text.MessageFormat which only supports numeric indices.
            // Convert named placeholders to numeric indices (preserving insertion order).
            List<Object> argsList = new ArrayList<>(namedValues.values());
            String indexedPattern = pattern;
            int idx = 0;
            for (String name : namedValues.keySet()) {
                // replace occurrences like {n1234,number} with {0,number}, etc.
                // Use literal replace; name should not contain regex-special chars.
                indexedPattern = indexedPattern.replace("{" + name + ",", "{" + idx + ",");
                idx++;
            }
            MessageFormat jmf = new MessageFormat(indexedPattern, locale);
            return jmf.format(argsList.toArray());
        }
    }
}