package com.godaddy.vps4.util.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SnapshotNameValidatorTest {

    Validator validator = ValidatorRegistry.getInstance().get("snapshot-name");

    @Test
    public void characterLength(){
        // must be 5-16 characters long

        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid("four"));
        assertTrue(validator.isValid("fivec"));
        assertTrue(validator.isValid("sevench"));
        assertTrue(validator.isValid("sixteensixteensi"));
        assertFalse(validator.isValid("seventeenseventee"));
        assertFalse(validator.isValid("17seventeenseventee"));
    }

    @Test
    public void allowLetters(){
        assertTrue(validator.isValid("upperCase")); // only one upper
        assertTrue(validator.isValid("UPPERCASE")); // all upper
        assertTrue(validator.isValid("UPPERcASE")); // only one lower
    }

    @Test
    public void alphaNumeric(){
        assertTrue(validator.isValid("camelCase01"));
        assertTrue(validator.isValid("5numberStart"));
        assertTrue(validator.isValid("1234567890"));
        assertFalse(validator.isValid("12345678901234567"));
    }

    @Test
    public void onlyValidSpecialChars(){
        assertTrue(validator.isValid("testcase")); // control case
        assertFalse(validator.isValid("test/case")); // / is invalid
        assertFalse(validator.isValid("test.case")); // dot is invalid
        assertTrue(validator.isValid("test_case")); // _ is valid
        assertTrue(validator.isValid("test-case")); // - is valid
        assertTrue(validator.isValid("tE5t_case")); // _ is valid
        assertTrue(validator.isValid("tE5t-case")); // - is valid
    }

}
