package com.godaddy.vps4.security.api.phase2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.web.security.SupportAuthenticationFilter;
import com.godaddy.vps4.web.security.Vps4SupportRequestAuthenticator;

public class SupportAuthenticationFilterTest {

    Vps4SupportRequestAuthenticator auth;
    HttpServletRequest request;
    HttpServletResponse response;
    StringWriter writer;
    FilterChain chain;
    
    
    @Before
    public void setUp() throws Exception {
        auth = mock(Vps4SupportRequestAuthenticator.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();
        chain = mock(FilterChain.class);
    }

    @After
    public void tearDown() throws Exception {
        auth = null;
        request = null;
        response = null;
        writer = null;
        chain = null;
    }

    @Test
    public void testAuth() {

        try {
            when(auth.authenticate(any(HttpServletRequest.class))).thenReturn(true);

            SupportAuthenticationFilter filter = new SupportAuthenticationFilter(auth);
            filter.doFilter(request, response, chain);
        }
        catch (IOException | ServletException e) {
            String failMessage = String.format("Unexpected exception in support authentication. Exception: {}", e);
            fail(failMessage);
        }

        try {
            verify(chain).doFilter(request, response);
        }
        catch (IOException | ServletException e) {
            String failMessage = String.format("Unexpected exception in support auth verification. Exception: {}", e);
            fail(failMessage);
        }
    }

    @Test
    public void testAuthFailed() {
        
        try {
            when(auth.authenticate(any(HttpServletRequest.class))).thenReturn(false);
            when(response.getWriter()).thenReturn(new PrintWriter(writer));

            SupportAuthenticationFilter filter = new SupportAuthenticationFilter(auth);

            filter.doFilter(request, response, chain);
        }
        catch (IOException | ServletException e) {
            String failMessage = String.format("Unexpected exception in support authentication. Exception: {}", e);
            fail(failMessage);
        }

        try {
            // since authenticator returned false, do not execute the filter chain
            verify(chain, times(0)).doFilter(request, response);
            verify(response, times(1)).setStatus(HttpServletResponse.SC_FORBIDDEN);

            JSONObject json = (JSONObject) new JSONParser().parse(writer.toString());
            assertEquals("Expected message id does not match actual id. ", "MISSING_SUPPORT_AUTHENTICATION", json.get("id"));
        }
        catch (IOException | ServletException | ParseException ex) {
            String failMessage = String.format("Unexpected exception in support auth verification. Exception: {}", ex);
            fail(failMessage);
        }
    }

}
