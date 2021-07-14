package com.godaddy.vps4.scheduler.security;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AuthenticationFilterTest {

    @Test
    public void testAuthenticated() throws Exception {
        XCertSubjectHeaderAuthenticator auth = mock(XCertSubjectHeaderAuthenticator.class);
        when(auth.authenticate(any(HttpServletRequest.class))).thenReturn(true);

        AuthenticationFilter filter = new AuthenticationFilter(auth);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    public void testUnauthenticated() throws Exception {
        XCertSubjectHeaderAuthenticator auth = mock(XCertSubjectHeaderAuthenticator.class);
        when(auth.authenticate(any(HttpServletRequest.class))).thenReturn(false);

        AuthenticationFilter filter = new AuthenticationFilter(auth);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        filter.doFilter(request, response, chain);

        verify(chain, times(0)).doFilter(request, response);

        JSONObject json = (JSONObject) new JSONParser().parse(writer.toString());
        assertEquals("MISSING_AUTHENTICATION", json.get("id"));
    }

    @Test
    public void testGeneralException() throws Exception {
        XCertSubjectHeaderAuthenticator auth = mock(XCertSubjectHeaderAuthenticator.class);
        when(auth.authenticate(any(HttpServletRequest.class))).thenReturn(true);

        AuthenticationFilter filter = new AuthenticationFilter(auth);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        Mockito.doThrow(new RuntimeException("something broke"))
                .when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);
        JSONObject json = (JSONObject) new JSONParser().parse(writer.toString());
        assertEquals("INTERNAL_ERROR", json.get("id"));
    }

}
