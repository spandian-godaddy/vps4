package com.godaddy.vps4.web.security.sso;

import com.nimbusds.jose.jwk.JWK;

/**
 * Fetch a key with a given Key ID
 * 
 * 
 * @author Brian Diekelman
 *
 */
public interface KeyService {

    JWK getKey(String keyId);
}
