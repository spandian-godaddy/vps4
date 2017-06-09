package com.godaddy.vps4.web.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.ThreadLocalRequestId;

/**
 *
 * Filter that extracts the X-Request-Id header from incoming requests
 * and -- if found -- attaches it to ThreadLocalRequestId for
 * the duration of the request.
 *
 * ThreadLocalRequestId can be used further down the call stack to retrieve
 * the request ID.
 *
 */
public class RequestIdFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestIdFilter.class);

    private static final int REQUEST_ID_MAX_LENGTH = 50;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {


    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest)) {
            return;
        }

        String requestId = ((HttpServletRequest)request).getHeader("X-Request-Id");
        if (requestId != null) {

            if (requestId.length() <= REQUEST_ID_MAX_LENGTH) {
                ThreadLocalRequestId.set(requestId);
            } else {
                logger.warn("Request ID header exceeds max length of {}", REQUEST_ID_MAX_LENGTH);
            }

        }

        try {
            chain.doFilter(request, response);

        } finally {
            // once the request chain completes, unset the thread-local request ID
            ThreadLocalRequestId.set(null);
        }
    }

    @Override
    public void destroy() {

    }

}
