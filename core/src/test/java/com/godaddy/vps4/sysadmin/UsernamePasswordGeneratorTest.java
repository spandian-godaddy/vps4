package com.godaddy.vps4.sysadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;

public class UsernamePasswordGeneratorTest {

    @Test
    public void testGeneratePassword() {
        int length = 14;
        String password = UsernamePasswordGenerator.generatePassword(length);
        assertEquals(length, password.length());
        char start = password.charAt(0);
        assertTrue(start >= 'a' && start <= 'z');

        PasswordValidator validator = new PasswordValidator(Arrays.asList(
                new LengthRule(length, length),

                // at least one upper-case character
                new CharacterRule(EnglishCharacterData.UpperCase, 1),

                // at least one lower-case character
                new CharacterRule(EnglishCharacterData.LowerCase, 1),

                // at least one digit character
                new CharacterRule(EnglishCharacterData.Digit, 1),

                // at least one symbol (special character)
                new CharacterRule(EnglishCharacterData.Special, 1),

                // no whitespace
                new WhitespaceRule()));

        RuleResult result = validator.validate(new PasswordData(password));
        assertTrue(result.isValid());
    }

    @Test
    public void testGenerateUsername() {
        int length = 12;
        String username = UsernamePasswordGenerator.generateUsername(length);
        assertEquals(length, username.length());

        PasswordValidator validator = new PasswordValidator(Arrays.asList(
                new LengthRule(length, length),

                // all lower-case character
                new CharacterRule(EnglishCharacterData.LowerCase, length),

                // no whitespace
                new WhitespaceRule()));

        RuleResult result = validator.validate(new PasswordData(username));
        assertTrue(result.isValid());
    }

}
