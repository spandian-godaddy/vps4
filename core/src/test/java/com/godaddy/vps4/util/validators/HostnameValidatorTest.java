package com.godaddy.vps4.util.validators;

import org.junit.Test;

import static org.junit.Assert.*;

public class HostnameValidatorTest {

    Validator validator = ValidatorRegistry.getInstance().get("hostname");

    @Test
    public void fqdn() {
    	// 3 sets of 1-15 characters separated by periods
        assertTrue(validator.isValid("fake.User-name1.test"));
        assertFalse(validator.isValid(".User-name1.test"));  // empty first section
        assertFalse(validator.isValid("fake.User-name1.test.test")); // 4 sections
        assertFalse(validator.isValid("fake.User-name1.")); // empty last section
        assertFalse(validator.isValid("fake.User-name1")); // only 2 sections
        assertFalse(validator.isValid("fake")); // only one section
        assertFalse(validator.isValid("")); // blank

    }
    
    @Test
    public void specialCharacters() {
        // . and - are the only allowed special characters 
        
        assertTrue(validator.isValid("fake.User-name1.test"));
        assertFalse(validator.isValid("fake.User@name1.test"));
        assertFalse(validator.isValid("fake.User!name1.test"));
        assertFalse(validator.isValid("fake.User#name1.test"));
        assertFalse(validator.isValid("fake.User$name1.test"));
    }
    
    @Test
    public void sectionLength() {
        // Less than 16 characters per section
        
        assertTrue(validator.isValid("fake12345678901.User-name101234.test12341234123")); //15 characters per section
        assertFalse(validator.isValid("fake12345678901a.User-name101234.test12341234123")); //16 characters in first section
        assertFalse(validator.isValid("fake12345678901.User-name101234a.test12341234123")); //16 characters in second section
        assertFalse(validator.isValid("fake12345678901.User-name101234.test12341234123a")); //16 characters in third section
    }
    
    @Test
    public void beginsWithHyphen() {
        // Doesn't begin with a hyphen
        
        assertTrue(validator.isValid("fake.User-name1.test"));
        assertFalse(validator.isValid("-fake.User-name1.test"));
    }
    
    @Test
    public void endsWithHyphen() {
        // Doesn't end with a hyphen
        
        assertTrue(validator.isValid("fake.User-name1.test"));
        assertFalse(validator.isValid("fake.User-name1.test-"));
    }
    
    @Test
    public void adjacentPeriods() {
        // Multiple periods may not be adjacent
        
        assertTrue(validator.isValid("fake.User-name1.test"));
        assertFalse(validator.isValid("fake..test")); // empty middle section
    }

    @Test
    public void adjacentHyphens() {
        // Multiple hyphens may not be adjacent
        
        assertTrue(validator.isValid("fake.User-name1.test"));
        assertFalse(validator.isValid("fake.User--name1.test")); // adjacent hyphens
    }
    

}
