package com.godaddy.vps4.web.security;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.vps4.Environment;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;

public class SsoRequestAuthenticator implements RequestAuthenticator<GDUser> {

    private final Logger logger = LoggerFactory.getLogger(SsoRequestAuthenticator.class);

    private final SsoTokenExtractor tokenExtractor;

    private final Vps4UserService userService;

    private final Config config;

    @Inject
    public SsoRequestAuthenticator(SsoTokenExtractor tokenExtractor, Vps4UserService userService,
            CreditService creditService, Config config) {
        this.tokenExtractor = tokenExtractor;
        this.userService = userService;
        this.config = config;
    }

    private boolean isStagingOrProductionEnv() {
        return ((Environment.CURRENT == Environment.STAGE) || (Environment.CURRENT == Environment.PROD) ? true : false);
    }

    private boolean isInternalShopper(Vps4User user) {
        return (user.getShopperId().length() == 3);
    }

    @Override
    public GDUser authenticate(HttpServletRequest request) {

        SsoToken token = tokenExtractor.extractToken(request);
        if (token == null) {
            return null;
        }

        String shopperOverride = request.getHeader("X-Shopper-Id");
        GDUser gdUser = new GDUser(token, shopperOverride);

        if (!gdUser.isStaff())
        {
            // TODO: Remove this after ECOMM integration
            Vps4User user = userService.getOrCreateUserForShopper(gdUser.getShopperId());

            boolean allow3LetterAccountsOnly = Boolean.parseBoolean(config.get("allow3LetterAccountsOnly", "true"));
            logger.info("Environment Staging or Production? : {}", isStagingOrProductionEnv());
            logger.info("Allow internal shoppers only: {}", allow3LetterAccountsOnly );
            if (isStagingOrProductionEnv() && allow3LetterAccountsOnly && !isInternalShopper(user)) {
                logger.warn("Non-3-letter shopper encountered in ALPHA release: {}", user);
                throw new RuntimeException("Currently only 3 letter accounts are allowed in ALPHA release. ");
            }
        }

        return gdUser;
    }
}
