package com.godaddy.vps4.web.security;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

public class Vps4CorsFilterTest {

    HttpServletRequest request;
    HttpServletResponse response;
    FilterChain chain;
    Vps4CorsFilter filter;
    String myhUrl;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        filter = new Vps4CorsFilter();
    }

    private void validateFilterAllowsCredentials(String url) throws Exception {
        when(request.getHeader("Origin")).thenReturn(url);
        when(request.getHeader("Access-Control-Request-Method")).thenReturn("GET");

        filter.doFilter(request, response, chain);
        verify(response, atLeastOnce()).setHeader("Access-Control-Allow-Origin", url);
        verify(response, atLeastOnce()).setHeader("Access-Control-Allow-Credentials", "true");
        verify(response, atLeastOnce()).setStatus(HttpServletResponse.SC_OK);
    }

    private void validateFilterBlocksCredentials(String url) throws Exception {
        when(request.getHeader("Origin")).thenReturn(url);
        when(request.getHeader("Access-Control-Request-Method")).thenReturn("GET");

        filter.doFilter(request, response, chain);
        verify(response, never()).setHeader("Access-Control-Allow-Origin", url);
        verify(response, never()).setHeader("Access-Control-Allow-Credentials", "true");
        verify(response, atLeastOnce()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testValidUrlSetsResponseHeaders() throws Exception {
        for (String url : getAllowedUrls()) {
            validateFilterAllowsCredentials(url);
        }
    }

    @Test
    public void testValidatesOrigin() throws Exception {
        for (String url : getBlockedUrls()) {
            validateFilterBlocksCredentials(url);
        }
    }

    private List<String> getAllowedUrls() {
        List<String> goodList = new ArrayList<>();
        goodList.add("https://myh.godaddy.com");
        goodList.add("http://myh.dev-godaddy.com");
        goodList.add("https://myh.test-godaddy.com");
        goodList.add("https://myh.stg-godaddy.com");
        goodList.add("https://myh.ote-godaddy.com");
        goodList.add("https://myh.secureserver.net");
        goodList.add("https://myh.dev-secureserver.net");
        goodList.add("https://myh.test-secureserver.net");
        goodList.add("https://myh.stg-secureserver.net");
        goodList.add("https://myh.ote-secureserver.net");
        return goodList;
    }

    private List<String> getBlockedUrls() {
        List<String> badList = new ArrayList<>();
        badList.add("https://myh.baddaddy.com");
        badList.add("https://myh.bad-godaddy.com");
        badList.add("https://myh.test-godaddy.net");
        badList.add("https://myh.stag-godaddy.com");
        badList.add("https://myh.godaddy.net");
        badList.add("https://myh.secureserver.com");
        badList.add("http://myh.dev-godadddy.com");
        badList.add("http://fraud.mygodaddy.com");
        badList.add("http://fraud.secureservernet.net");
        return badList;
    }

    @Test
    public void testAccessControlRequestMethodNotSet() throws Exception {
        when(request.getHeader("Origin")).thenReturn("https://myh.godaddy.com");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testAccessControlOriginNotSet() throws Exception {
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void callInitAndDestroy() throws Exception {
        // Validate they don't throw exceptions
        filter.init(mock(FilterConfig.class));
        filter.destroy();
    }

}
