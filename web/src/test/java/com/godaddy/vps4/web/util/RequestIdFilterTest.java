package com.godaddy.vps4.web.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.util.ThreadLocalRequestId;

public class RequestIdFilterTest {

    RequestIdFilter filter;
    HttpServletRequest request;
    HttpServletResponse response;
    FilterChain chain;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        filter = new RequestIdFilter();
    }

    @Test
    public void testRequestIdAttach() throws Exception {

        String requestId = UUID.randomUUID().toString();

        when(request.getHeader("X-Request-Id")).thenReturn(requestId);

        final AtomicReference<String> requestIdWithinRequestChain = new AtomicReference<>();

        doAnswer(invocation -> {

            requestIdWithinRequestChain.set(ThreadLocalRequestId.get());
            return null;

        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertEquals("thread-local request ID is set during request",
                        requestId, requestIdWithinRequestChain.get());

        assertNull("thread-local request ID is unset after request",
                        ThreadLocalRequestId.get());
    }

    @Test
    public void testRequestIdBadLength() throws Exception {

        String requestId = String.join("", Collections.nCopies(51, "a"));

        when(request.getHeader("X-Request-Id")).thenReturn(requestId);

        final AtomicReference<String> requestIdWithinRequestChain = new AtomicReference<>();

        doAnswer(invocation -> {

            requestIdWithinRequestChain.set(ThreadLocalRequestId.get());
            return null;

        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertNull("thread-local request ID is NOT set during request",
                        requestIdWithinRequestChain.get());

        assertNull("thread-local request ID is unset after request",
                        ThreadLocalRequestId.get());
    }
}
