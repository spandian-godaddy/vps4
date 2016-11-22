package com.godaddy.vps4.web.security.sso.token;

import java.util.List;

public class JomaxSsoToken extends SsoToken {

    public static final String REALM = "jomax";

    List<String> groups; // realm=jomax only

    public List<String> getGroups() {
        return groups;
    }

    @Override
    public String getRealm() {
        return REALM;
    }

}
