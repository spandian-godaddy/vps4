package com.godaddy.vps4.util.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.apache.commons.text.RandomStringGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

public class PasswordValidatorTest {
    /*
     * be 8-48 characters long (8 or more)
     * starts with a letter
     * include a lower case letter
     * include an upper case letter
     * include a number
     * include a special character (! @ # $ %)
     * cannot include certain special characters (&,?,;)
     * cannot include spaces
     * cannot include admin
     * cannot include international characters
     */
    private static enum EnumPasswordCharacters {

        UPPERCASE("ABCDEFGHIJKLMNOPQRSTUVWXYZ"), LOWERCASE("abcdefghijklmnopqrstuvwxyz"), NUMERIC(
                "1234567890"), SPECIAL("!@#$%"), DISALLOWED_SPECIAL("&?;");

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

    private static String generateInvalidString(String prefixCharsToUse, String charsToUse) {
        RandomStringGenerator generator = new RandomStringGenerator.Builder().selectFrom(prefixCharsToUse.toCharArray())
                .build();
        String strPrefix = generator.generate(1);
        generator = new RandomStringGenerator.Builder().selectFrom(charsToUse.toCharArray()).build();
        String restOfString = generator.generate(randInt(7, 13));
        return String.format("%s%s", strPrefix, restOfString);
    }

    private static String appendToValidPassword(String suffix) {
        String charsToUse = String.format("%s%s%s%s", EnumPasswordCharacters.LOWERCASE, EnumPasswordCharacters.SPECIAL,
                EnumPasswordCharacters.NUMERIC, EnumPasswordCharacters.UPPERCASE);

        RandomStringGenerator lowerCaseGenerator = new RandomStringGenerator.Builder()
                .selectFrom(EnumPasswordCharacters.LOWERCASE.toString().toCharArray()).build();
        RandomStringGenerator upperCaseGenerator = new RandomStringGenerator.Builder()
                .selectFrom(EnumPasswordCharacters.UPPERCASE.toString().toCharArray()).build();
        RandomStringGenerator numericCaseGenerator = new RandomStringGenerator.Builder()
                .selectFrom(EnumPasswordCharacters.NUMERIC.toString().toCharArray()).build();
        RandomStringGenerator specialCaseGenerator = new RandomStringGenerator.Builder()
                .selectFrom(EnumPasswordCharacters.SPECIAL.toString().toCharArray()).build();

        String strPrefixMatchingReqdRules = String.format("%s%s%s%s", lowerCaseGenerator.generate(1),
                upperCaseGenerator.generate(1), numericCaseGenerator.generate(1), specialCaseGenerator.generate(1));

        RandomStringGenerator restGenerator = new RandomStringGenerator.Builder().selectFrom(charsToUse.toCharArray())
                .build();
        String restOfString = restGenerator.generate(4, 10);
        return String.format("%s%s%s", strPrefixMatchingReqdRules, restOfString, suffix);
    }

    @BeforeClass
    public static void getPasswordValidator() {
        validator = ValidatorRegistry.getInstance().get("password");
    }

    @Test
    public void shouldBeValidLowerBound() {
        String charsToUse = String.format("%s%s%s%s", EnumPasswordCharacters.UPPERCASE,
                EnumPasswordCharacters.LOWERCASE, EnumPasswordCharacters.SPECIAL, EnumPasswordCharacters.NUMERIC);
        RandomStringGenerator generator = new RandomStringGenerator.Builder().selectFrom(charsToUse.toCharArray())
                .build();
        String invalidString = generator.generate(randInt(0, 7));

        boolean isValid = validator.isValid(invalidString);

        assertFalse(isValid);
    }

    @Test
    public void invalidValidUpperBound() {
        String charsToUse = String.format("%s%s%s%s", EnumPasswordCharacters.UPPERCASE,
                EnumPasswordCharacters.LOWERCASE, EnumPasswordCharacters.SPECIAL, EnumPasswordCharacters.NUMERIC);
        RandomStringGenerator generator = new RandomStringGenerator.Builder().selectFrom(charsToUse.toCharArray())
                .build();
        String invalidString = generator.generate(55);

        boolean isValid = validator.isValid(invalidString);

        assertFalse(isValid);
    }

    @Test
    public void shouldIncludeALowercaseLetter() {
        String prefixCharsToUse = String.format("%s", EnumPasswordCharacters.UPPERCASE);
        String charsToUse = String.format("%s%s%s", EnumPasswordCharacters.UPPERCASE, EnumPasswordCharacters.SPECIAL,
                EnumPasswordCharacters.NUMERIC);

        boolean isValid = validator.isValid(generateInvalidString(prefixCharsToUse, charsToUse));

        assertFalse(isValid);
    }

    @Test
    public void shouldIncludeAUppercaseLetter() {
        String prefixCharsToUse = String.format("%s", EnumPasswordCharacters.LOWERCASE);
        String charsToUse = String.format("%s%s%s", EnumPasswordCharacters.LOWERCASE, EnumPasswordCharacters.SPECIAL,
                EnumPasswordCharacters.NUMERIC);

        boolean isValid = validator.isValid(generateInvalidString(prefixCharsToUse, charsToUse));

        assertFalse(isValid);
    }

    @Test
    public void shouldIncludeANumber() {
        String prefixCharsToUse = String.format("%s%s", EnumPasswordCharacters.UPPERCASE,
                EnumPasswordCharacters.LOWERCASE);
        String charsToUse = String.format("%s%s%s", EnumPasswordCharacters.LOWERCASE, EnumPasswordCharacters.SPECIAL,
                EnumPasswordCharacters.UPPERCASE);

        boolean isValid = validator.isValid(generateInvalidString(prefixCharsToUse, charsToUse));

        assertFalse(isValid);
    }

    @Test
    public void shouldIncludeASpecialCharacter() {
        String prefixCharsToUse = String.format("%s%s", EnumPasswordCharacters.UPPERCASE,
                EnumPasswordCharacters.LOWERCASE);
        String charsToUse = String.format("%s%s%s", EnumPasswordCharacters.LOWERCASE, EnumPasswordCharacters.NUMERIC,
                EnumPasswordCharacters.UPPERCASE);

        boolean isValid = validator.isValid(generateInvalidString(prefixCharsToUse, charsToUse));

        assertFalse(isValid);
    }

    @Test
    public void shouldNotIncludeDisallowedSpecialCharacter() {
        String disallowedCharsToUse = String.format("%s",
                    EnumPasswordCharacters.DISALLOWED_SPECIAL);
        RandomStringGenerator generator = new RandomStringGenerator.Builder().selectFrom(disallowedCharsToUse.toCharArray()).build();

        boolean isValid = validator.isValid(appendToValidPassword(generator.generate(1)));

        assertFalse(isValid);
    }

    @Test
    public void validPassword() {
        assertTrue(validator.isValid(appendToValidPassword("")));
    }

    @Test
    public void shouldNotIncludeAdmin() {
        validator = ValidatorRegistry.getInstance().get("password");
        char[] adminArr = "admin".toCharArray();
        int randChar = randInt(0, 4);
        adminArr[randChar] = Character.toUpperCase(adminArr[randChar]);

        boolean isValid = validator.isValid(appendToValidPassword(new String(adminArr)));

        assertFalse(isValid);
    }
    @Test
    public void shouldNotIncludeSpaces() {
        validator = ValidatorRegistry.getInstance().get("password");
        char[] spaceArr = "space".toCharArray();
        int randChar = randInt(0, 4);
        spaceArr[randChar] = ' ';

        boolean isValid = validator.isValid(appendToValidPassword(new String(spaceArr)));

        assertFalse(isValid);
    }
    @Test
    public void shouldNotIncludeSpecialCharacter() {
        validator = ValidatorRegistry.getInstance().get("password");

        boolean isValid = validator.isValid(appendToValidPassword("ßigMäuse"));

        assertFalse(isValid);
    }
}
