package com.gpl.rpg.AndorsTrail.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.junit.Test;

public final class LocalizedNumberFormatterTest {

    @Test
    public void formatsDigitBlocksWithLocaleSeparators() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);

            String formattedWithSpaces = LocalizedNumberFormatter.parseString(
                    "Gold: { 1234567 }, XP: { -42 }", Locale.GERMANY);
            String formattedWithoutSpaces = LocalizedNumberFormatter.parseString(
                    "Gold: {1234567}, XP: {-42}", Locale.GERMANY);
            String formattedFloatWithSpaces = LocalizedNumberFormatter.parseString(
                    "Speed: { 12.5 }, Crit: { -0.75 }", Locale.GERMANY);
            String formattedFloatWithoutSpaces = LocalizedNumberFormatter.parseString(
                    "Speed: {12.5}, Crit: {-0.75}", Locale.GERMANY);

            assertEquals("Gold: 1.234.567, XP: -42", formattedWithSpaces);
            assertEquals("Gold: 1.234.567, XP: -42", formattedWithoutSpaces);
            assertEquals("Speed: 12,5, Crit: -0,75", formattedFloatWithSpaces);
            assertEquals("Speed: 12,5, Crit: -0,75", formattedFloatWithoutSpaces);
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void leavesNonDigitTextUntouched() {
        assertEquals("No placeholders here", LocalizedNumberFormatter.parseString(
                "No placeholders here", Locale.US));
    }

    @Test
    public void returnsNullForNullInput() {
        assertNull(LocalizedNumberFormatter.parseString(null, Locale.US));
    }
}
