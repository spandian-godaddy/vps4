package com.godaddy.vps4.network;

import org.junit.Test;

import com.godaddy.vps4.Vps4Exception;

public class IpAddressValidatorTest {

    @Test
    public void testValidateIpAddress() {
        IpAddressValidator.validateIpAddress("192.168.1.1");
    }

    @Test(expected = Vps4Exception.class)
    public void testValidateIpAddressInvalidCharacter() {
        IpAddressValidator.validateIpAddress("a92.168.1.1");
    }

}
