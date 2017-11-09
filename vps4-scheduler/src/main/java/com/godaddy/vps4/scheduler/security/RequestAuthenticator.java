package com.godaddy.vps4.scheduler.security;

import javax.servlet.http.HttpServletRequest;


public interface RequestAuthenticator<T> {

    T authenticate(HttpServletRequest request);

}
