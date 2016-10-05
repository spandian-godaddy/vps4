package com.godaddy.vps4.util.validators;

import java.util.Random;
import org.junit.Test;
import org.junit.BeforeClass;
import org.apache.commons.lang3.RandomStringUtils;

import static org.junit.Assert.*;

public class PasswordValidatorTest {
    /*
     * be 8-14 characters long (8 or more)
     * starts with a letter
     * include a lower case letter
     * include an upper case letter
     * include a number
     * include a special character (! @ # $ %)
     * cannot include certain special characters (&,?,;)
     */

    private static enum EnumPasswordCharacters {

        UPPERCASE("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        LOWERCASE("abcdefghijklmnopqrstuvwxyz"),
        NUMERIC("1234567890"),
        SPECIAL("!@#$%"),
        DISALLOWED_SPECIAL("&?;");

        private String characters;

        @Override
        public String toString() {
          return this.characters;
        }

        private EnumPasswordCharacters(String characters) {
          this.characters = characters;
        }
    }

    static Validator validator;

    static int randInt(int min, int max) {
        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    @BeforeClass
    public static void getPasswordValidator() {
        validator = ValidatorRegistry.getInstance().get("password");
    }

    @Test
    public void shouldBeValidLowerBound() {
        String charsToUse = String.format("%s%s%s%s",
                    EnumPasswordCharacters.UPPERCASE,
                    EnumPasswordCharacters.LOWERCASE,
                    EnumPasswordCharacters.SPECIAL,
                    EnumPasswordCharacters.NUMERIC);
        String invalidString = RandomStringUtils.random(randInt(0, 7), charsToUse.toCharArray());
        assertFalse(validator.isValid(invalidString));
    }

    @Test
    public void invalidValidUpperBound() {
        String charsToUse = String.format("%s%s%s%s",
                    EnumPasswordCharacters.UPPERCASE,
                    EnumPasswordCharacters.LOWERCASE,
                    EnumPasswordCharacters.SPECIAL,
                    EnumPasswordCharacters.NUMERIC);
        String invalidString = RandomStringUtils.random(15, charsToUse.toCharArray());
        assertFalse(validator.isValid(invalidString));
    }

    @Test
    public void shouldStartWithALetter() {
        String prefixCharsToUse = String.format("%s%s",
                    EnumPasswordCharacters.SPECIAL,
                    EnumPasswordCharacters.NUMERIC);
        String charsToUse = String.format("%s%s%s%s",
                    EnumPasswordCharacters.UPPERCASE,
                    EnumPasswordCharacters.LOWERCASE,
                    EnumPasswordCharacters.SPECIAL,
                    EnumPasswordCharacters.NUMERIC);

        String strPrefix = RandomStringUtils.random(1, prefixCharsToUse);
        String restOfString = RandomStringUtils.random(randInt(7, 13), charsToUse.toCharArray());
        String invalidString = String.format("%s%s", strPrefix, restOfString);

        assertFalse(validator.isValid(invalidString));
    }

    @Test
    public void shouldIncludeALowercaseLetter() {
        String prefixCharsToUse = String.format("%s",
                    EnumPasswordCharacters.UPPERCASE);
        String charsToUse = String.format("%s%s%s",
                    EnumPasswordCharacters.UPPERCASE,
                    EnumPasswordCharacters.SPECIAL,
                    EnumPasswordCharacters.NUMERIC);

        String strPrefix = RandomStringUtils.random(1, prefixCharsToUse);
        String restOfString = RandomStringUtils.random(randInt(7, 13), charsToUse.toCharArray());
        String invalidString = String.format("%s%s", strPrefix, restOfString);

        assertFalse(validator.isValid(invalidString));
    }

    @Test
    public void shouldIncludeAUppercaseLetter() {
        String prefixCharsToUse = String.format("%s",
                    EnumPasswordCharacters.LOWERCASE);
        String charsToUse = String.format("%s%s%s",
                    EnumPasswordCharacters.LOWERCASE,
                    EnumPasswordCharacters.SPECIAL,
                    EnumPasswordCharacters.NUMERIC);

        String strPrefix = RandomStringUtils.random(1, prefixCharsToUse);
        String restOfString = RandomStringUtils.random(randInt(7, 13), charsToUse.toCharArray());
        String invalidString = String.format("%s%s", strPrefix, restOfString);

        assertFalse(validator.isValid(invalidString));
    }

    @Test
    public void shouldIncludeANumber() {
        String prefixCharsToUse = String.format("%s%s",
                    EnumPasswordCharacters.UPPERCASE,
                    EnumPasswordCharacters.LOWERCASE);
        String charsToUse = String.format("%s%s%s",
                    EnumPasswordCharacters.LOWERCASE,
                    EnumPasswordCharacters.SPECIAL,
                    EnumPasswordCharacters.UPPERCASE);

        String strPrefix = RandomStringUtils.random(1, prefixCharsToUse);
        String restOfString = RandomStringUtils.random(randInt(7, 13), charsToUse.toCharArray());
        String invalidString = String.format("%s%s", strPrefix, restOfString);

        assertFalse(validator.isValid(invalidString));
    }

    @Test
    public void shouldIncludeASpecialCharacter() {
        String prefixCharsToUse = String.format("%s%s",
                    EnumPasswordCharacters.UPPERCASE,
                    EnumPasswordCharacters.LOWERCASE);
        String charsToUse = String.format("%s%s%s",
                    EnumPasswordCharacters.LOWERCASE,
                    EnumPasswordCharacters.NUMERIC,
                    EnumPasswordCharacters.UPPERCASE);

        String strPrefix = RandomStringUtils.random(1, prefixCharsToUse);
        String restOfString = RandomStringUtils.random(randInt(7, 13), charsToUse.toCharArray());
        String invalidString = String.format("%s%s", strPrefix, restOfString);

        assertFalse(validator.isValid(invalidString));
    }

    @Test
    public void shouldNotIncludeDisallowedSpecialCharacter() {
        String prefixCharsToUse = String.format("%s%s",
                    EnumPasswordCharacters.UPPERCASE,
                    EnumPasswordCharacters.LOWERCASE);
        String charsToUse = String.format("%s%s%s",
                    EnumPasswordCharacters.LOWERCASE,
                    EnumPasswordCharacters.SPECIAL,
                    EnumPasswordCharacters.NUMERIC,
                    EnumPasswordCharacters.UPPERCASE);
        String disallowedCharsToUse = String.format("%s",
                    EnumPasswordCharacters.DISALLOWED_SPECIAL);

        String strPrefix = RandomStringUtils.random(1, prefixCharsToUse);
        String restOfString = RandomStringUtils.random(randInt(6, 12), charsToUse.toCharArray());
        String disallowedString = RandomStringUtils.random(1, disallowedCharsToUse);
        String invalidString = String.format("%s%s%s", strPrefix, restOfString, disallowedString);

        assertFalse(validator.isValid(invalidString));
    }

    @Test
    public void validPassword() {
        String charsToUse = String.format("%s%s%s",
                    EnumPasswordCharacters.LOWERCASE,
                    EnumPasswordCharacters.SPECIAL,
                    EnumPasswordCharacters.NUMERIC,
                    EnumPasswordCharacters.UPPERCASE);

        String strPrefixMatchingReqdRules = String.format("%s%s%s%s",
                RandomStringUtils.random(1, EnumPasswordCharacters.LOWERCASE.toString().toCharArray()),
                RandomStringUtils.random(1, EnumPasswordCharacters.UPPERCASE.toString().toCharArray()),
                RandomStringUtils.random(1, EnumPasswordCharacters.NUMERIC.toString().toCharArray()),
                RandomStringUtils.random(1, EnumPasswordCharacters.SPECIAL.toString().toCharArray()));

        String restOfString = RandomStringUtils.random(randInt(4, 10), charsToUse.toCharArray());
        String validString = String.format("%s%s", strPrefixMatchingReqdRules, restOfString);

        assertTrue(validator.isValid(validString));
    }
}
