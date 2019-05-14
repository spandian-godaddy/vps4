package com.godaddy.vps4.web.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.web.Vps4Exception;

public class WebUtilsTest {
    HttpHeaders httpHeaders = mock(HttpHeaders.class);
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    String xFwdedIpAddress = "1.1.1.1";
    String remoteIpAddress = "2.2.2.2";

    @Before
    public void setUp() throws Exception {
        List<String> xForwadedHdr = new ArrayList<>();
        xForwadedHdr.add(xFwdedIpAddress);
        when(httpHeaders.getRequestHeader("X-Forwarded-For")).thenReturn(xForwadedHdr);
        when(httpServletRequest.getRemoteAddr()).thenReturn(remoteIpAddress);
    }

    @Test
    public void getClientIpAddressWhenXForwardedHdrSet() throws Exception {
        String retIpAddress = Utils.getClientIpAddress(httpHeaders, httpServletRequest);
        assertEquals(xFwdedIpAddress, retIpAddress);
    }

    @Test
    public void getClientIpAddressWhenNoXFwdHdrButRemoteAddrSet() throws Exception {
        when(httpHeaders.getRequestHeader("X-Forwarded-For")).thenReturn(new ArrayList<>());
        String retIpAddress = Utils.getClientIpAddress(httpHeaders, httpServletRequest);
        assertEquals(remoteIpAddress, retIpAddress);
    }

    @Test(expected = Vps4Exception.class)
    public void getClientIpThrowsExceptionWhenIpAddressCantBeDetermined() throws Exception {
        when(httpHeaders.getRequestHeader("X-Forwarded-For")).thenReturn(new ArrayList<>());
        when(httpServletRequest.getRemoteAddr()).thenReturn("");
        Utils.getClientIpAddress(httpHeaders, httpServletRequest);
    }

    @Test(expected = Vps4Exception.class)
    public void getClientIpThrowsExceptionWhenIpAddressIsIpv6() throws Exception {
        when(httpHeaders.getRequestHeader("X-Forwarded-For")).thenReturn(new ArrayList<>());
        when(httpServletRequest.getRemoteAddr()).thenReturn("0:0:0:0:0:1");
        Utils.getClientIpAddress(httpHeaders, httpServletRequest);
    }

    @Test
    public void isIpv4AddressReturnsTrueForAnIpv4Address() throws Exception {
        assertTrue(Utils.isIPv4Address("1.1.1.1"));
        assertTrue(Utils.isIPv4Address("127.0.0.1"));
        assertTrue(Utils.isIPv4Address("192.168.1.12"));
    }

    @Test
    public void isIpv4AddressReturnsFalseForANonIpv4Address() throws Exception {
        assertFalse(Utils.isIPv4Address("notanip"));
        assertFalse(Utils.isIPv4Address("0:0:0:0:0:1"));
    }

}