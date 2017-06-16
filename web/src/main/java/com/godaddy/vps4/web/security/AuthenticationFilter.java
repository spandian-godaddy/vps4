package com.godaddy.vps4.web.security;

import java.io.IOException;
import java.io.Writer;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.web.Vps4Exception;

/**
 * Examine a request using authenticators, attach the first authenticated user found to the a request attribute.
 *
 * No additional action is taken if an authenticated user is _not_ found, since all downstream actions may not necessarily require
 * authentication, it's up to downstream to take that action (like, for example, redirect to the SSO login page).
 */
public class AuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    public static final String USER_ATTRIBUTE_NAME = "sso-user";

    final RequestAuthenticator<GDUser> authenticator;

    @Inject
    public AuthenticationFilter(Vps4RequestAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;

        try {
            GDUser user = authenticator.authenticate(request);
            if (user != null) {
                request.setAttribute(USER_ATTRIBUTE_NAME, user);
                chain.doFilter(req, res);
                return;
            }

            if (res instanceof HttpServletResponse) {
                HttpServletResponse response = (HttpServletResponse) res;

                JSONObject json = new JSONObject();
                json.put("id", "MISSING_AUTHENTICATION");
                json.put("message", "No SSO token was found in your request");

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                try (Writer writer = response.getWriter()) {
                    writer.write(json.toJSONString());
                }
            }

        }
        catch (Vps4Exception e) {
            JSONObject json = new JSONObject();
            json.put("id", e.getId());
            json.put("message", e.getMessage());

            HttpServletResponse response = (HttpServletResponse) res;
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            try (Writer writer = response.getWriter()) {
                writer.write(json.toJSONString());
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
