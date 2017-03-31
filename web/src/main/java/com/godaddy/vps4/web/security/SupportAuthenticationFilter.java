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

public class SupportAuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SupportAuthenticationFilter.class);
    
    final RequestAuthenticator<Boolean> authenticator; 
    
    @Inject
    public SupportAuthenticationFilter(Vps4SupportRequestAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        
        logger.info("Attempting to check if request is from a support admin...");
        
        HttpServletRequest request = (HttpServletRequest) req;

        try {
            // check if user is a support administrator
            Boolean isSupportAdmin = authenticator.authenticate(request);
            if (isSupportAdmin) {
                // user was verified as a support administrator.
                logger.info("Verified user as support admin...");
                chain.doFilter(req, res);
                return;
            }

            if (res instanceof HttpServletResponse) {
                HttpServletResponse response = (HttpServletResponse) res;

                JSONObject json = new JSONObject();
                json.put("id", "MISSING_SUPPORT_AUTHENTICATION");
                json.put("message", "SSO token does not allow support level access");

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
            logger.warn("Exception in support request authenticator", e);

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
