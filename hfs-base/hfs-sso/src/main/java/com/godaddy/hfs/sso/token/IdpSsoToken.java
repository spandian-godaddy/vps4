package com.godaddy.hfs.sso.token;

public class IdpSsoToken extends SsoToken {

    public static final String REALM = "idp";

    String shopperId;

    String customerId;

    String privateLabelId;

    public String getShopperId() {
        return shopperId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getPrivateLabelId() {
        return privateLabelId;
    }

    @Override
    public String getRealm() {
        return REALM;
    }

}
