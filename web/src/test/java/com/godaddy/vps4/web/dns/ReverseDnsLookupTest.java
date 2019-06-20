package com.godaddy.vps4.web.dns;

import org.junit.Test;

import com.godaddy.vps4.web.Vps4Exception;

public class ReverseDnsLookupTest {

    private ReverseDnsLookup reverseDnsLookup = new ReverseDnsLookup();

    @Test(expected = Vps4Exception.class)
    public void throwsExceptionIfIpAddressIsNotProvided() {
        reverseDnsLookup.validateReverseDnsName("fake-reverse-dns-name", "");
    }

    @Test(expected = Vps4Exception.class)
    public void throwsExceptionIfReverseDnsNameIsNotProvided() {
        reverseDnsLookup.validateReverseDnsName("", "1.2.3.4");
    }

    @Test(expected = Vps4Exception.class)
    public void throwsExceptionIfReverseDnsNameLookupFails() {
        reverseDnsLookup.validateReverseDnsName("google.com", "1.2.3.4");
    }

    @Test(expected = Vps4Exception.class)
    public void throwsExceptionIfReverseDnsNameLookupHasBadDomainName() {
        reverseDnsLookup.validateReverseDnsName("wrong-host-name", "1.2.3.4");
    }

    @Test
    public void testReverseDnsNameLookup() {
        reverseDnsLookup.validateReverseDnsName("godaddy.com", "208.109.192.70");
    }
}