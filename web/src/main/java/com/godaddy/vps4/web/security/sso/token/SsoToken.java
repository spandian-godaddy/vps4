package com.godaddy.vps4.web.security.sso.token;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public abstract class SsoToken {

    public static final String USER_TYPE_EMPLOYEE = "EMPLOYEE";
    public static final String USER_TYPE_DELEGATE = "DELEGATE";
    public static final String USER_TYPE_SHOPPER = "SHOPPER";

    public SignedJWT jwt;

    public ReadOnlyJWTClaimsSet claims;

    String firstName;

    String lastName;

    String authType; // basic | ??

    String username;

    long accessLevel;

    String displayName;
    String userType;

    public SsoToken delegateUser;
    public SsoToken employeeUser;

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public SignedJWT getJwt() {
        return jwt;
    }

    public abstract String getRealm();

    public String getAuthType() {
        return authType;
    }

    public String getUsername() {
        return username;
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
        return "SsoToken [jwt=" + jwt + ", firstName=" + firstName + ", lastName=" + lastName + ", realm=" + getRealm()
                + ", authType=" + authType + ", username=" + username + "]";
    }

}
