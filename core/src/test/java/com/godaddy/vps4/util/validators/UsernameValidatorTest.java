package com.godaddy.vps4.util.validators;

import org.junit.Test;

import static org.junit.Assert.*;

public class UsernameValidatorTest {

    Validator validator = ValidatorRegistry.getInstance().get("username");

    @Test
    public void characterLength(){
    	// must be 5-16 characters long

    	assertFalse(validator.isValid(""));
    	assertFalse(validator.isValid("four"));
    	assertTrue(validator.isValid("fivec"));
    	assertTrue(validator.isValid("sevench"));
    	assertTrue(validator.isValid("sixteensixteensi"));
    	assertFalse(validator.isValid("seventeenseventee"));
    }

    @Test
    public void allLowerLetters(){
    	assertFalse(validator.isValid("upperCase")); // only one upper
    	assertFalse(validator.isValid("UPPERCASE")); // all upper
    	assertFalse(validator.isValid("UPPERcASE")); // only one lower
    }

    @Test
    public void onlyValidSpecialChars(){
    	assertTrue(validator.isValid("testcase")); // control case
    	assertFalse(validator.isValid("test/case")); // / is invalid
    	assertFalse(validator.isValid("test.case")); // / is invalid
    	assertTrue(validator.isValid("test_case")); // _ is valid
    	assertTrue(validator.isValid("test-case")); // - is valid
    }

    @Test
    public void illegalNames(){
    	assertFalse(validator.isValid("root"));
    	assertFalse(validator.isValid("admin"));
    	assertFalse(validator.isValid("administrator"));
    	assertFalse(validator.isValid("users"));
    	assertFalse(validator.isValid("user"));
    	assertFalse(validator.isValid("system"));
    	assertFalse(validator.isValid("group"));
    	assertFalse(validator.isValid("cpaneldemo"));

    	assertTrue(validator.isValid("root1"));
    	assertTrue(validator.isValid("1root"));
    	assertTrue(validator.isValid("1root1"));
    }



}
