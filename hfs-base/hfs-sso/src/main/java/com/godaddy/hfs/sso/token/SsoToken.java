package com.godaddy.hfs.sso.token;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public abstract class SsoToken {

    public static final String USER_TYPE_EMPLOYEE = "EMPLOYEE";
    public static final String USER_TYPE_DELEGATE = "DELEGATE";
    public static final String USER_TYPE_SHOPPER = "SHOPPER";

    public SignedJWT jwt;

    public ReadOnlyJWTClaimsSet claims;

    String authType; // basic | ??

    long accessLevel;

    String displayName;
    String userType;

    public SsoToken delegateUser;
    public SsoToken employeeUser;
    public SsoToken certificateToken;

    public SignedJWT getJwt() {
        return jwt;
    }

    public abstract String getRealm();

    public String getAuthType() {
        return authType;
    }

    public long getAccessLevel() {
        return accessLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUserType() {
        return userType;
    }

    @Override
    public String toString() {
        return "SsoToken [jwt=" + jwt + ", realm=" + getRealm() + ", authType=" + authType + "]";
    }

}
