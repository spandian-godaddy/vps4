package com.godaddy.vps4.util.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HostnameValidatorTest {

    Validator validator = ValidatorRegistry.getInstance().get("hostname");

    @Test
    public void fqdn() {
        // 3 sets of 1-15 characters separated by periods
        assertTrue(validator.isValid("fake.User-name1.test"));
        assertTrue(validator.isValid("ip-111-112-113-114.ip.secureserver.net")); // 4 sections
        assertFalse(validator.isValid(".User-name1.test"));  // empty first section
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
        // Less than 64 characters per section

        String longSection = "iamtheproudownerofthelongestlongestlongestdomainnameinthisworld";
        assertEquals(63, longSection.length());
        assertTrue(validator.isValid(longSection + ".secureserver.net"));  //63 chars in first section
        assertTrue(validator.isValid("subdomain." + longSection + ".net"));  //63 chars in middle section
        assertFalse(validator.isValid(longSection + "1.secureserver.net")); //64 chars not allowed!
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

    @Test
    public void shouldNotStartWithW3Prefix() {
        // Cannot begin www. prefix

        assertFalse(validator.isValid("www.is-invalid.test"));
        assertTrue(validator.isValid("the.www-in-middle.test"));
        assertTrue(validator.isValid("ww.is-valid.test"));
        assertTrue(validator.isValid("wwww.is-also-valid.test"));
    }

    @Test
    public void cpanelHostnameShouldNotStartWithCpanelOrWHM() {
        // Cpanel hostnames cannot begin with cpanel or whm prefix
        Validator validator = ValidatorRegistry.getInstance().get("cpanelHostname");

        assertFalse(validator.isValid("cpanel.is-invalid.test"));
        assertFalse(validator.isValid("CPaNel.is-invalid.test"));
        assertTrue(validator.isValid("the.cpanel-in-middle.test"));
        assertTrue(validator.isValid("thiscpanel.is-valid.test"));
        assertTrue(validator.isValid("cpanellll.is-valid.test"));

        assertFalse(validator.isValid("whm.is-invalid.test"));
        assertFalse(validator.isValid("WHM.is-invalid.test"));
        assertTrue(validator.isValid("the.whm-in-middle.test"));
        assertTrue(validator.isValid("thiswhm.is-valid.test"));
        assertTrue(validator.isValid("whmmm.is-also-valid.test"));
    }

}
