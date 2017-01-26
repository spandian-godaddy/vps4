package com.godaddy.vps4.web.security;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.Environment;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachineService;

public class Vps4RequestAuthenticator implements RequestAuthenticator {

    private final Logger logger = LoggerFactory.getLogger(Vps4RequestAuthenticator.class);

    private final SsoTokenExtractor tokenExtractor;

    private final Vps4UserService userService;

    private final VirtualMachineService virtualMachineService;

    private final Config config;

    @Inject
    public Vps4RequestAuthenticator(SsoTokenExtractor tokenExtractor, Vps4UserService userService,
            VirtualMachineService virtualMachineService, Config config) {
        this.tokenExtractor = tokenExtractor;
        this.userService = userService;
        this.virtualMachineService = virtualMachineService;
        this.config = config;
    }

    private boolean isStagingOrProductionEnv() {
        return ((Environment.CURRENT == Environment.STAGE) || (Environment.CURRENT == Environment.PROD) ? true : false);
    }

    private boolean isInternalShopper(Vps4User user, String shopperId) {
        return (user.getShopperId().length() == 3);
    }

    @Override
    public Vps4User authenticate(HttpServletRequest request) {

        SsoToken token = tokenExtractor.extractToken(request);
        if (token == null) {
            return null;
        }

        // TODO break out auth into separate tables for each type (map to mcs_user)
        // so one table for mcs_user_idp, mcs_user_jomax, etc
        String shopperId = token.getUsername();
        if (token instanceof IdpSsoToken) {
            shopperId = ((IdpSsoToken) token).getShopperId();
        }

        Vps4User user = userService.getOrCreateUserForShopper(shopperId);

        // TODO: Remove this after ECOMM integration
        boolean allow3LetterAccountsOnly = Boolean.parseBoolean(config.get("allow3LetterAccountsOnly", "true"));
        logger.info("Environment Staging or Production? : {}" , isStagingOrProductionEnv());
        logger.info("Allow internal shoppers only: {}", allow3LetterAccountsOnly );
        if (isStagingOrProductionEnv() && allow3LetterAccountsOnly && !isInternalShopper(user, shopperId)) {
            logger.warn("Non-3-letter shopper encountered in ALPHA release: {}", user);
            throw new RuntimeException("Currently only 3 letter accounts are allowed in ALPHA release. ");
        }

        virtualMachineService.createCreditIfNoneExists(user);
        return user;
    }
}
