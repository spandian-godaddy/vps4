package com.godaddy.vps4.client;

import javax.inject.Inject;

import com.godaddy.hfs.config.*;

// This is a dummy implementation which just returns a hard coded jwt which can be provided using the local
// configuration file. This token is then forwarded into the request, that can be used to call the web micro-service.
// Ideally this service would log into GoDaddy SSO service to obtain a JWT and cache it till it expires.
public class SsoTokenServiceImpl implements SsoTokenService {
    @Inject
    public Config config;

    @Override
    public String getJwt() {
        return config.get("ssoJwt", "REPLACE_CONFIG_ENTRY_WITH_SSO_JWT_TOKEN");
    }
}
