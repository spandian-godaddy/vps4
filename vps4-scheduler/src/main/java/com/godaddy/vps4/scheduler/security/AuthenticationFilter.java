package com.godaddy.vps4.scheduler.security;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Examine a request using authenticators,
 *
 * No additional action is taken if a request is authenticated, since all downstream actions may not necessarily require
 * authentication, it's up to downstream to take that action.
 */
public class AuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    public static final String USER_ATTRIBUTE_NAME = "sso-user";

    final RequestAuthenticator<Boolean> authenticator;

    @Inject
    public AuthenticationFilter(RequestAuthenticator<Boolean> authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    @SuppressWarnings("unchecked")
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        try {
            if (authenticator.authenticate(request)) {
                chain.doFilter(req, res);
                return;
            }
            else{
                HttpServletResponse response = (HttpServletResponse) res;

                JSONObject json = new JSONObject();
                json.put("id", "MISSING_AUTHENTICATION");
                json.put("message", "The request did not send the correct authentication.");

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                try (Writer writer = response.getWriter()) {
                    writer.write(json.toJSONString());
                }
            }
        }
        catch (Exception e) {
            logger.warn("Exception in request authenticator", e);

            JSONObject json = new JSONObject();
            json.put("id", "INTERNAL_ERROR");
            json.put("message", "An internal error occurred");

            HttpServletResponse response = (HttpServletResponse) res;
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            try (Writer writer = response.getWriter()) {
                writer.write(json.toJSONString());
            }
        }

    }

    @Override
    public void destroy() {

    }

}
