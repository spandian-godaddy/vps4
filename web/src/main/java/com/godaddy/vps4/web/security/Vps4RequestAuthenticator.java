package com.godaddy.vps4.web.security;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.security.sso.SsoTokenExtractor;
import com.godaddy.vps4.web.security.sso.token.IdpSsoToken;
import com.godaddy.vps4.web.security.sso.token.SsoToken;

public class Vps4RequestAuthenticator implements RequestAuthenticator {

    private final Logger logger = LoggerFactory.getLogger(Vps4RequestAuthenticator.class);

    private final SsoTokenExtractor tokenExtractor;

    private final Vps4UserService userService;

    private final VirtualMachineService virtualMachineService;

    public Vps4RequestAuthenticator(SsoTokenExtractor tokenExtractor, Vps4UserService userService,
            VirtualMachineService virtualMachineService) {
        this.tokenExtractor = tokenExtractor;
        this.userService = userService;
        this.virtualMachineService = virtualMachineService;
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
        virtualMachineService.createOrionRequestIfNoneExists(user);

        return user;
    }
}
