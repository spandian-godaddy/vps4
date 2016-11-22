package com.godaddy.vps4.web.security;

import javax.servlet.http.HttpServletRequest;

import com.godaddy.vps4.security.Vps4User;

public interface RequestAuthenticator {

    Vps4User authenticate(HttpServletRequest request);

}
