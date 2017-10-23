package com.godaddy.vps4.client;

// TODO: this interface probably needs to be fleshed out further
// for now, this has a single method, returning the string version of a godaddy SSO jwt
public interface SsoTokenService {
    String getJwt();
}
