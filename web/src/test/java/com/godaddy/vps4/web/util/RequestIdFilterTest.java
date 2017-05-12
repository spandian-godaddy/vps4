package com.godaddy.vps4.web.util;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.godaddy.vps4.util.ThreadLocalRequestId;

import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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

        UUID requestId = UUID.randomUUID();

        when(request.getHeader("X-Request-Id")).thenReturn(requestId.toString());

        final AtomicReference<UUID> requestIdWithinRequestChain = new AtomicReference<>();

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
    public void testBadRequestId() throws Exception {

        when(request.getHeader("X-Request-Id")).thenReturn("not-a-valid-uuid");

        filter.doFilter(request, response, chain);

        assertNull("thread-local request is not set", ThreadLocalRequestId.get());

        // filter chain is still invoked (ignoring bad request ID)
        verify(chain).doFilter(request, response);
    }
}
