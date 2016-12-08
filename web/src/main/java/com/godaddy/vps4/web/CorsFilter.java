package com.godaddy.vps4.web;

import java.io.IOException;

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

public class CorsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorsFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        String origin = request.getHeader("Origin");
        if (origin != null) {

            if (request.getHeader("Access-Control-Request-Method") != null) {

                logger.debug("pre-flight CORS request: {}", origin);

                // Note: we're allowing any origin here with the assumption
                //       that the actual resource will process the 'Authorization'
                //       header when the actual request occurs.
                //       We could probably also process the Authorization header
                //       in the pre-flight request
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, PATCH");
                response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");

                // don't allow the client to send us cookies
                response.setHeader("Access-Control-Allow-Credentials", "false");

                // don't process any downstream filters, those will be handled
                // when the request itself (not the pre-flight) comes in

                // greenlight the pre-flight request
                response.setStatus(200);
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
