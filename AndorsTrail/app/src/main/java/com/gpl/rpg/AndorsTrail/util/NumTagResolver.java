package com.gpl.rpg.AndorsTrail.util;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve <num>...</num> tags in translated strings and replace them with
 * properly localized numeric strings. The tag syntax supported is:
 *   <num>1234</num>
 *   <num fmt='...'>-42</num>
 *
 * Only integer values are supported (the tag parser accepts an optional leading
 * '-' sign). The optional fmt attribute is supported and controls formatting:
 *   - fmt="int" (default): use project's Format.localizeInt() when possible
 *     (falls back to NumberFormat for large values)
 *   - fmt="nosep": localize digits but do not emit grouping separators
 * Any other fmt values currently fall back to the default behavior.
 */
public final class NumTagResolver {
    private NumTagResolver() { }

    // Matches: <num>1234</num> or <num fmt='int'>1234</num> (single or double quotes)
    // Groups: 1 = quote (unused), 2 = fmt, 3 = digits
    private static final Pattern NUM_TAG_RE = Pattern.compile(
            "<num(?:\\s+fmt\\s*=\\s*(['\"])([^'\">]+)\\1)?\\s*>(-?\\d+)</num>",
            Pattern.CASE_INSENSITIVE);

    public static String resolveNumTags(String input) {
        if (input == null || input.isEmpty()) return input;

        Matcher m = NUM_TAG_RE.matcher(input);
        StringBuffer out = new StringBuffer(input.length() + 16);
        Locale locale = Locale.getDefault();

        while (m.find()) {
            String fmt = m.group(2); // the fmt attribute value (if present)
            String digits = m.group(3);

            if(fmt == null || fmt.isEmpty()) fmt = "int"; // default if fmt is missing or empty
            fmt = fmt.toLowerCase();
            switch(fmt) {
                case "int":
                case "nosep":
                    break; // supported formats, no change needed
                default:
                    fmt = "int"; // unknown formats fall back to default
            }

            String replacement;
            try {
                // Try parse as long first (fast path)
                long v = Long.parseLong(digits);

                // For larger numbers, use NumberFormat with locale
                NumberFormat nf = NumberFormat.getIntegerInstance(locale);
                if (fmt.equals("nosep")) nf.setGroupingUsed(false);
                replacement = nf.format(v);
            } catch (NumberFormatException e) {
                // If parsing as long fails (too large), try BigInteger
                try {
                    BigInteger bi = new BigInteger(digits);
                    NumberFormat nf = NumberFormat.getIntegerInstance(locale);
                    if (fmt.equals("nosep")) nf.setGroupingUsed(false);
                    replacement = nf.format(bi);
                } catch (Exception ex) {
                    // if anything goes wrong, fall back to the original digits
                    replacement = digits;
                }
            }

            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }
}
