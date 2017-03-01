package com.godaddy.vps4.network;

import org.junit.Test;

public class IpAddressValidatorTest {

    @Test
    public void testValidateIpAddress() {
        IpAddressValidator.validateIpAddress("192.168.1.1");
    }

    @Test(expected = RuntimeException.class)
    public void testValidateIpAddressInvalidCharacter() {
        IpAddressValidator.validateIpAddress("a92.168.1.1");
    }

}
