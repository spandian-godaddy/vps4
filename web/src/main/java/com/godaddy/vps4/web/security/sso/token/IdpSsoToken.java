package com.godaddy.vps4.web.security.sso.token;

public class IdpSsoToken extends SsoToken {

    private static final String REALM = "idp";

    String shopperId;

    String privateLabelId;

    public String getShopperId() {
        return shopperId;
    }

    public String getPrivateLabelId() {
        return privateLabelId;
    }

    @Override
    public String getRealm() {
        return REALM;
    }

}
