package com.godaddy.vps4.sysadmin;

import java.util.ArrayList;
import java.util.List;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

public class UsernamePasswordGenerator {

    private static final String SPECIAL_CHARACTERS = "@!#%$";
    private static final CharacterData SPECIAL_CHAR_DATA = new CharacterData() {
        @Override
        public String getErrorCode() {
            return "INVALID_SPECIAL_CHARS";
        }

        @Override
        public String getCharacters() {
            return SPECIAL_CHARACTERS;
        }
    };
    
    public static String generateUsername(String usernamePrefix, int totalLength) {
        int lengthDifference = totalLength - usernamePrefix.length();
        if(lengthDifference < 1) {
            throw new RuntimeException("Usernames are restricted to 12 characters. Please configure a prefix that is 11 characters or less.");
        }

        List<CharacterRule> rules = new ArrayList<>();
        rules.add(new CharacterRule(EnglishCharacterData.LowerCase, 1));

        PasswordGenerator pwGenerator = new PasswordGenerator();
        return usernamePrefix + pwGenerator.generatePassword(lengthDifference, rules);
    }
    
    public static String generatePassword(int length) {
        List<CharacterRule> rules = new ArrayList<>();
        rules.add(new CharacterRule(EnglishCharacterData.UpperCase, 1));
        rules.add(new CharacterRule(EnglishCharacterData.LowerCase, 1));
        rules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
        rules.add(new CharacterRule(SPECIAL_CHAR_DATA, 1));
        
        List<CharacterRule> firstLetterRules = new ArrayList<>();
        firstLetterRules.add(new CharacterRule(EnglishCharacterData.LowerCase, 1));

        PasswordGenerator pwGenerator = new PasswordGenerator();
        String firstLetter = pwGenerator.generatePassword(1, firstLetterRules);

        return firstLetter + pwGenerator.generatePassword(length - 1, rules);
    }
}
