package com.godaddy.hfs.sso;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;

import java.util.List;

/**
 * Fetch a key with a given Key ID
 * 
 * 
 * @author Brian Diekelman
 *
 */
public interface SsoService {

    JWK getKey(String keyId);

    List<String> getGroups(SignedJWT jwt);
}
