package com.godaddy.vps4.security;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.godaddy.vps4.web.security.RequestAuthenticator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.AuthenticationFilter;
import com.godaddy.vps4.web.security.SsoRequestAuthenticator;

public class AuthenticationFilterTest {

    Set<RequestAuthenticator<GDUser>> auths = new HashSet<RequestAuthenticator<GDUser>>();

    @Test
    public void testAuthenticated() throws Exception {
        GDUser gdUser = GDUserMock.createShopper("shopperId");
        SsoRequestAuthenticator auth = mock(SsoRequestAuthenticator.class);
        when(auth.authenticate(any(HttpServletRequest.class))).thenReturn(gdUser);

        auths.add(auth);

        AuthenticationFilter filter = new AuthenticationFilter(auths);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(request).setAttribute("sso-user", gdUser);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testUnauthenticated() throws Exception {
        GDUser gdUser = null;

        SsoRequestAuthenticator auth = mock(SsoRequestAuthenticator.class);
        when(auth.authenticate(any(HttpServletRequest.class))).thenReturn(gdUser);
        auths.add(auth);

        AuthenticationFilter filter = new AuthenticationFilter(auths);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // shouldn't execute the filter chain if we're not authenticated
        verify(chain, times(0)).doFilter(request, response);

        JSONObject json = (JSONObject) new JSONParser().parse(writer.toString());
        assertEquals("MISSING_AUTHENTICATION", json.get("id"));
    }

    @Test
    public void testVps4Exception() throws Exception {
        GDUser gdUser = GDUserMock.createShopper("shopperId");

        SsoRequestAuthenticator auth = mock(SsoRequestAuthenticator.class);
        when(auth.authenticate(any(HttpServletRequest.class))).thenReturn(gdUser);

        auths.add(auth);

        AuthenticationFilter filter = new AuthenticationFilter(auths);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        FilterChain chain = mock(FilterChain.class);

        Mockito.doThrow(new Vps4Exception("SOME_VPS4_EXCEPTION", "something broke"))
                .when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        JSONObject json = (JSONObject) new JSONParser().parse(writer.toString());
        assertEquals("SOME_VPS4_EXCEPTION", json.get("id"));
    }

    @Test
    public void testGeneralException() throws Exception {
        GDUser gdUser = GDUserMock.createShopper("shopperId");

        SsoRequestAuthenticator auth = mock(SsoRequestAuthenticator.class);
        when(auth.authenticate(any(HttpServletRequest.class))).thenReturn(gdUser);

        auths.add(auth);

        AuthenticationFilter filter = new AuthenticationFilter(auths);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        FilterChain chain = mock(FilterChain.class);

        Mockito.doThrow(new RuntimeException("something broke"))
                .when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        JSONObject json = (JSONObject) new JSONParser().parse(writer.toString());
        assertEquals("INTERNAL_ERROR", json.get("id"));
    }

}
