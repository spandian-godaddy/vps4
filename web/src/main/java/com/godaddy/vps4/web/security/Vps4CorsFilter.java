package com.godaddy.vps4.web.security;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vps4CorsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(Vps4CorsFilter.class);

    private final Pattern allowedOriginRegex = Pattern.compile(".*\\.(dev-|test-|stg-|ote-)?(godaddy\\.com|secureserver\\.net)$");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        logger.info("Vps4CorsFilter");
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        String origin = request.getHeader("Origin");
        if (origin != null) {

            // Validate origin
            if (!allowedOriginRegex.matcher(origin).matches()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, PATCH");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");

            // allows the client to send us cookies
            response.setHeader("Access-Control-Allow-Credentials", "true");

            if (request.getHeader("Access-Control-Request-Method") != null) {

                logger.debug("vps4 pre-flight CORS request: {}", origin);

                // don't process any downstream filters, those will be handled
                // when the request itself (not the pre-flight) comes in

                // greenlight the pre-flight request
                response.setStatus(SC_OK);
                return;
            }
        }

        // we didn't match on any of the pre-flight CORS features,
        // so do the rest of the filter chain
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

}