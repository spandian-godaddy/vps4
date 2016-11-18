package com.godaddy.vps4.web.security.sso.token;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import net.minidev.json.JSONObject;

/**
 * realm=JOMAX: header: {"alg":"RS256","kid":"Z96wwimmoQ"} payload: { "factors": {"k_pw": 1438041184}, "firstname": "Brian", "lastname":
 * "Diekelman", "auth": "basic", "ftc": 1, "jti": "TjW9ICV9JsufXf47xHcJtw", "iat": 1438041184, "typ": "jomax", "accountName": "bdiekelman",
 * "groups": ["Toolzilla-HOC", "Development"] }
 *
 * realm=IDP: header: {"alg":"RS256","kid":"Z96wwimmoQ"} payload: { "factors": {"k_pw": 1438122627}, "firstname": "Brian", "lastname":
 * "Diekelman", "auth": "basic", "ftc": 1, "jti": "1ILRZN8gCWoJtOdDcIIVmw", "iat": 1438122627, "typ": "idp", "username": "5y5", "shopperId":
 * "5y5", "plid": "1" }
 *
 * @author Brian Diekelman
 *
 */
public class SsoTokenReader {

    private static final Logger logger = LoggerFactory.getLogger(SsoTokenReader.class);

    private static final String TYPE_IDP = "auth_idp";

    private static final String TYPE_JOMAX = "auth_jomax";

    private static final String AUTH_TYPE_BASIC = "basic";

    private static final String AUTH_TYPE_SHOPPER_TO_SHOPPER = "s2s";

    private static final String AUTH_TYPE_EMPOLYEE_TO_SHOPPER = "e2s";

    private static final String AUTH_TYPE_EMPLOYEE_TO_SHOPPER_TO_SHOPPER = "e2s2s";

    public SsoToken readSsoToken(Cookie cookie) throws ParseException {

        if (cookie.getName().equals(TYPE_JOMAX)
                || cookie.getName().equals(TYPE_IDP)) {

            return readSsoToken(cookie.getValue());
        }
        return null;
    }

    public SsoToken readSsoToken(String value) throws ParseException {
        SignedJWT jwt = SignedJWT.parse(value);

        return readSsoToken(jwt);
    }

    public SsoToken readSsoToken(SignedJWT jwt) throws ParseException {

        ReadOnlyJWTClaimsSet claims = jwt.getJWTClaimsSet();

        logger.info("claims: {}", claims);

        String auth = claims.getStringClaim("auth");
        if (auth == null || auth.isEmpty())
            throw new ParseException("Could not parse auth type from jwt.", 0);

        if (auth.equalsIgnoreCase(SsoTokenReader.AUTH_TYPE_BASIC)) {
            return GetBasicSsoToken(claims, jwt);
        }
        else if (auth.equalsIgnoreCase(SsoTokenReader.AUTH_TYPE_SHOPPER_TO_SHOPPER)) {
            return GetShopperToShopperSsoToken(claims, jwt);
        }
        else if (auth.equalsIgnoreCase(SsoTokenReader.AUTH_TYPE_EMPOLYEE_TO_SHOPPER)) {
            return GetEmployeeToShopperSsoToken(claims, jwt);
        }
        else if (auth.equalsIgnoreCase(SsoTokenReader.AUTH_TYPE_EMPLOYEE_TO_SHOPPER_TO_SHOPPER)) {
            return GetEmployeeToShopperToShopperSsoToken(claims, jwt);
        }
        else {
            throw new ParseException("Unknown auth type " + auth + " in JWT", 0);
        }
    }

    private SsoToken GetEmployeeToShopperToShopperSsoToken(ReadOnlyJWTClaimsSet claims, SignedJWT jwt) throws ParseException {

        Map<String, Object> customClaims = claims.getCustomClaims();
        logger.info("e2s claims: {}", customClaims);

        JomaxSsoToken jomaxToken = new JomaxSsoToken();
        JSONObject jsonClaim = claims.toJSONObject();
        JSONObject outterDelegateClaim = (JSONObject) jsonClaim.get("del");
        JSONObject delegateClaim = (JSONObject) outterDelegateClaim.get("del");
        String type = (String) delegateClaim.get("typ");
        if (type == null || !type.equalsIgnoreCase(jomaxToken.getRealm())) {
            logger.warn("Invalid JOMAX JWT: {}", jwt);
            return null;
        }
        String username = (String) delegateClaim.get("accountName");

        if (username == null) {
            logger.warn("Invalid del jomax JWT: {}", jwt);
            return null;
        }

        // TYPE JOMAX
        jomaxToken.username = username;
        // common fields
        jomaxToken.jwt = jwt;
        jomaxToken.claims = claims;
        jomaxToken.firstName = (String) delegateClaim.get("firstname");
        jomaxToken.lastName = (String) delegateClaim.get("lastname");
        jomaxToken.authType = (String) delegateClaim.get("auth");
        jomaxToken.userType = SsoToken.USER_TYPE_EMPLOYEE;
        // TODO:Add the groups

        // Shopper Delegate
        IdpSsoToken shopperDelegateIdpToken = new IdpSsoToken();
        JSONObject shopperDelegateClaim = (JSONObject) outterDelegateClaim.get("e2s");
        String e2sType = (String) outterDelegateClaim.get("typ");
        if (e2sType == null || !e2sType.equalsIgnoreCase(shopperDelegateIdpToken.getRealm())) {
            logger.warn("Invalid idp JWT: {}", jwt);
            return null;
        }
        String shopperDelegateUsername = (String) shopperDelegateClaim.get("username");
        String shopperId = (String) shopperDelegateClaim.get("shopperId");

        if (username == null || shopperId == null) {
            logger.warn("Invalid del idp JWT: {}", jwt);
            return null;
        }

        // TYPE_IDP
        shopperDelegateIdpToken.username = shopperDelegateUsername;
        shopperDelegateIdpToken.shopperId = shopperId;
        shopperDelegateIdpToken.privateLabelId = String.valueOf(shopperDelegateClaim.get("plid"));
        // common fields
        shopperDelegateIdpToken.jwt = jwt;
        shopperDelegateIdpToken.claims = claims;
        shopperDelegateIdpToken.firstName = (String) shopperDelegateClaim.get("firstname");
        shopperDelegateIdpToken.lastName = (String) shopperDelegateClaim.get("lastname");
        shopperDelegateIdpToken.authType = (String) shopperDelegateClaim.get("auth");
        shopperDelegateIdpToken.userType = SsoToken.USER_TYPE_DELEGATE;

        // Get the e2s2s claim
        IdpSsoToken e2s2sIdpToken = new IdpSsoToken();
        JSONObject s2sClaim = (JSONObject) jsonClaim.get("e2s2s");
        String s2sType = (String) jsonClaim.get("typ");
        if (s2sType == null || !s2sType.equalsIgnoreCase(e2s2sIdpToken.getRealm())) {
            logger.warn("Invalid idp JWT: {}", jwt);
            return null;
        }
        String s2sUsername = (String) s2sClaim.get("username");
        String s2sShopperId = (String) s2sClaim.get("shopperId");

        if (s2sUsername == null || s2sShopperId == null) {
            logger.warn("Invalid e2s idp JWT: {}", jwt);
            return null;
        }

        // TYPE_IDP
        e2s2sIdpToken.username = s2sUsername;
        e2s2sIdpToken.shopperId = s2sShopperId;
        e2s2sIdpToken.privateLabelId = String.valueOf(s2sClaim.get("plid"));
        // common fields
        e2s2sIdpToken.jwt = jwt;
        e2s2sIdpToken.claims = claims;
        e2s2sIdpToken.firstName = (String) s2sClaim.get("firstname");
        e2s2sIdpToken.lastName = (String) s2sClaim.get("lastname");
        e2s2sIdpToken.userType = SsoToken.USER_TYPE_SHOPPER;
        e2s2sIdpToken.delegateUser = shopperDelegateIdpToken;// Add the Delegate Shopper Token
        e2s2sIdpToken.employeeUser = jomaxToken;// Add the Employee Delegate Token
        // always return the Shopper Token
        return e2s2sIdpToken;
    }

    private SsoToken GetEmployeeToShopperSsoToken(ReadOnlyJWTClaimsSet claims, SignedJWT jwt) {
        Map<String, Object> customClaims = claims.getCustomClaims();
        logger.info("e2s claims: {}", customClaims);

        JomaxSsoToken jomaxToken = new JomaxSsoToken();
        JSONObject jsonClaim = claims.toJSONObject();
        JSONObject delegateClaim = (JSONObject) jsonClaim.get("del");
        String type = (String) delegateClaim.get("typ");
        if (type == null || !type.equalsIgnoreCase(jomaxToken.getRealm())) {
            logger.warn("Invalid JOMAX JWT: {}", jwt);
            return null;
        }
        String username = (String) delegateClaim.get("accountName");

        if (username == null) {
            logger.warn("Invalid del jomax JWT: {}", jwt);
            return null;
        }

        // TYPE JOMAX
        jomaxToken.username = username;
        // common fields
        jomaxToken.jwt = jwt;
        jomaxToken.claims = claims;
        jomaxToken.firstName = (String) delegateClaim.get("firstname");
        jomaxToken.lastName = (String) delegateClaim.get("lastname");
        jomaxToken.authType = (String) delegateClaim.get("auth");
        jomaxToken.userType = SsoToken.USER_TYPE_EMPLOYEE;
        // TODO:Add the groups

        // Get the e2s claim
        IdpSsoToken e2sIdpToken = new IdpSsoToken();
        JSONObject s2sClaim = (JSONObject) jsonClaim.get("e2s");
        String s2sType = (String) jsonClaim.get("typ");
        if (s2sType == null || !s2sType.equalsIgnoreCase(e2sIdpToken.getRealm())) {
            logger.warn("Invalid idp JWT: {}", jwt);
            return null;
        }
        String s2sUsername = (String) s2sClaim.get("username");
        String s2sShopperId = (String) s2sClaim.get("shopperId");

        if (s2sUsername == null || s2sShopperId == null) {
            logger.warn("Invalid e2s idp JWT: {}", jwt);
            return null;
        }

        // TYPE_IDP
        e2sIdpToken.username = s2sUsername;
        e2sIdpToken.shopperId = s2sShopperId;
        e2sIdpToken.privateLabelId = String.valueOf(s2sClaim.get("plid"));
        // common fields
        e2sIdpToken.jwt = jwt;
        e2sIdpToken.claims = claims;
        e2sIdpToken.firstName = (String) s2sClaim.get("firstname");
        e2sIdpToken.lastName = (String) s2sClaim.get("lastname");
        e2sIdpToken.userType = SsoToken.USER_TYPE_SHOPPER;
        e2sIdpToken.employeeUser = jomaxToken;// Add the Employee Delegate Token
        // always return the Shopper Token
        return e2sIdpToken;
    }

    private SsoToken GetShopperToShopperSsoToken(ReadOnlyJWTClaimsSet claims, SignedJWT jwt) {
        Map<String, Object> customClaims = claims.getCustomClaims();
        logger.info("S2S claims: {}", customClaims);

        IdpSsoToken idpToken = new IdpSsoToken();
        JSONObject jsonClaim = claims.toJSONObject();
        JSONObject delegateClaim = (JSONObject) jsonClaim.get("del");
        String type = (String) delegateClaim.get("typ");
        if (type == null || !type.equalsIgnoreCase(idpToken.getRealm())) {
            logger.warn("Invalid idp JWT: {}", jwt);
            return null;
        }
        String username = (String) delegateClaim.get("username");
        String shopperId = (String) delegateClaim.get("shopperId");

        if (username == null || shopperId == null) {
            logger.warn("Invalid del idp JWT: {}", jwt);
            return null;
        }

        // TYPE_IDP
        idpToken.username = username;
        idpToken.shopperId = shopperId;
        idpToken.privateLabelId = String.valueOf(delegateClaim.get("plid"));
        // common fields
        idpToken.jwt = jwt;
        idpToken.claims = claims;
        idpToken.firstName = (String) delegateClaim.get("firstname");
        idpToken.lastName = (String) delegateClaim.get("lastname");
        idpToken.authType = (String) delegateClaim.get("auth");
        idpToken.userType = SsoToken.USER_TYPE_DELEGATE;

        // Get the s2s claim
        IdpSsoToken s2sIdpToken = new IdpSsoToken();
        JSONObject s2sClaim = (JSONObject) jsonClaim.get("s2s");
        String s2sType = (String) jsonClaim.get("typ");
        if (s2sType == null || !s2sType.equalsIgnoreCase(s2sIdpToken.getRealm())) {
            logger.warn("Invalid idp JWT: {}", jwt);
            return null;
        }
        String s2sUsername = (String) s2sClaim.get("username");
        String s2sShopperId = (String) s2sClaim.get("shopperId");

        if (s2sUsername == null || s2sShopperId == null) {
            logger.warn("Invalid s2s idp JWT: {}", jwt);
            return null;
        }

        // TYPE_IDP

        s2sIdpToken.username = s2sUsername;
        s2sIdpToken.shopperId = s2sShopperId;
        s2sIdpToken.privateLabelId = String.valueOf(s2sClaim.get("plid"));
        // common fields
        s2sIdpToken.jwt = jwt;
        s2sIdpToken.claims = claims;
        s2sIdpToken.firstName = (String) s2sClaim.get("firstname");
        s2sIdpToken.lastName = (String) s2sClaim.get("lastname");
        s2sIdpToken.userType = SsoToken.USER_TYPE_SHOPPER;
        s2sIdpToken.accessLevel = (long) s2sClaim.get("al");
        // //int accessLevel = 0;
        // //if(al != null && !al.isEmpty())
        // // Integer.parseInt(al);
        // s2sIdpToken.accessLevel = accessLevel;
        s2sIdpToken.displayName = (String) s2sClaim.get("disp");
        s2sIdpToken.delegateUser = idpToken;// Add the Delegate Shopper Token
        // always return the Shopper Token
        return s2sIdpToken;
    }

    private SsoToken GetBasicSsoToken(ReadOnlyJWTClaimsSet claims, SignedJWT jwt) throws ParseException {
        SsoToken ssoToken = null;
        String type = claims.getStringClaim("typ");

        if (type == null) {
            return null;
        }

        if (type.equals("jomax")) {
            // TYPE_JOMAX
            String username = claims.getStringClaim("accountName");
            List<String> groups = claims.getStringListClaim("groups");

            JomaxSsoToken jomaxToken = new JomaxSsoToken();
            jomaxToken.username = username;
            jomaxToken.groups = groups;
            ssoToken = jomaxToken;

        }
        else if (type.equals("idp")) {

            String username = claims.getStringClaim("username");
            String shopperId = claims.getStringClaim("shopperId");

            if (username == null || shopperId == null) {
                logger.warn("Invalid JOMAX JWT: {}", jwt);
                return null;
            }

            // TYPE_IDP
            IdpSsoToken idpToken = new IdpSsoToken();
            idpToken.username = username;
            idpToken.shopperId = shopperId;
            idpToken.privateLabelId = String.valueOf(claims.getClaim("plid"));
            ssoToken = idpToken;

        }
        else {
            logger.warn("Unknown JWT type: {}", type);
            return null;
        }

        // common fields
        ssoToken.jwt = jwt;
        ssoToken.claims = claims;
        ssoToken.firstName = claims.getStringClaim("firstname");
        ssoToken.lastName = claims.getStringClaim("lastname");
        ssoToken.authType = claims.getStringClaim("auth");
        return ssoToken;
    }
}
