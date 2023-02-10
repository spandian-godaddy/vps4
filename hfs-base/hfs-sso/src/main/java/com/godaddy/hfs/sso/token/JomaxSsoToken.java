package com.godaddy.hfs.sso.token;

import java.util.List;

public class JomaxSsoToken extends SsoToken {

    public static final String REALM = "jomax";

    String username;

    List<String> groups; // realm=jomax only

    public String getUsername() {
        return username;
    }

    public List<String> getGroups() {
        return groups;
    }

    @Override
    public String getRealm() {
        return REALM;
    }

}
