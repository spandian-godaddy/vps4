package com.godaddy.hfs.sso.token;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import com.godaddy.hfs.sso.SsoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * realm=JOMAX: header: {"alg":"RS256","kid":"Z96wwimmoQ"} payload: { "factors": {"k_pw": 1438041184}, "firstname": "Brian", "lastname":
 * "Diekelman", "auth": "basic", "ftc": 1, "jti": "TjW9ICV9JsufXf47xHcJtw", "iat": 1438041184, "typ": "jomax", "accountName": "bdiekelman",
 * "groups": ["Toolzilla-HOC", "Development"] }
 *
 * realm=IDP: header: {"alg":"RS256","kid":"Z96wwimmoQ"} payload: { "factors": {"k_pw": 1438122627},
 * "auth": "basic", "ftc": 1, "jti": "1ILRZN8gCWoJtOdDcIIVmw", "iat": 1438122627, "typ": "idp", "shopperId":
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
    private static final String AUTH_TYPE_CERTIFICATE_TO_SHOPPER = "cert2s";

    private final SsoService ssoService;

    public SsoTokenReader(SsoService ssoService) {
        this.ssoService = ssoService;
    }

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

            return getBasicSsoToken(claims, jwt);
        }
        else if (auth.equalsIgnoreCase(SsoTokenReader.AUTH_TYPE_SHOPPER_TO_SHOPPER)) {
            return getShopperToShopperSsoToken(claims, jwt);
        }
        else if (auth.equalsIgnoreCase(SsoTokenReader.AUTH_TYPE_EMPOLYEE_TO_SHOPPER)) {
            return getEmployeeToShopperSsoToken(claims, jwt);
        }
        else if (auth.equalsIgnoreCase(SsoTokenReader.AUTH_TYPE_EMPLOYEE_TO_SHOPPER_TO_SHOPPER)) {
            return getEmployeeToShopperToShopperSsoToken(claims, jwt);
        }
        else if(auth.equalsIgnoreCase(SsoTokenReader.AUTH_TYPE_CERTIFICATE_TO_SHOPPER)) {
            return getCertificateToShopperSsoToken(claims, jwt);
        }
        else {
            throw new ParseException("Unknown auth type " + auth + " in JWT", 0);
        }
    }

    private SsoToken getEmployeeToShopperToShopperSsoToken(ReadOnlyJWTClaimsSet claims, SignedJWT jwt) throws ParseException {

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
        jomaxToken.authType = (String) delegateClaim.get("auth");
        jomaxToken.userType = SsoToken.USER_TYPE_EMPLOYEE;
        JSONArray groups = (JSONArray) delegateClaim.get("groups");
        jomaxToken.groups = ssoService.getGroups(jwt);

        // Shopper Delegate
        IdpSsoToken shopperDelegateIdpToken = new IdpSsoToken();
        JSONObject shopperDelegateClaim = (JSONObject) outterDelegateClaim.get("e2s");
        String e2sType = (String) outterDelegateClaim.get("typ");
        if (e2sType == null || !e2sType.equalsIgnoreCase(shopperDelegateIdpToken.getRealm())) {
            logger.warn("Invalid idp JWT: {}", jwt);
            return null;
        }
        String shopperId = (String) shopperDelegateClaim.get("shopperId");
        String customerId = (String) shopperDelegateClaim.get("cid");

        if (shopperId == null) {
            logger.warn("Invalid del idp JWT: {}", jwt);
            return null;
        }

        // TYPE_IDP
        shopperDelegateIdpToken.shopperId = shopperId;
        shopperDelegateIdpToken.customerId = customerId;
        shopperDelegateIdpToken.privateLabelId = String.valueOf(shopperDelegateClaim.get("plid"));
        // common fields
        shopperDelegateIdpToken.jwt = jwt;
        shopperDelegateIdpToken.claims = claims;
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
        String s2sShopperId = (String) s2sClaim.get("shopperId");
        String s2sCustomerId = (String) s2sClaim.get("cid");

        if (s2sShopperId == null) {
            logger.warn("Invalid e2s idp JWT: {}", jwt);
            return null;
        }

        // TYPE_IDP
        e2s2sIdpToken.shopperId = s2sShopperId;
        e2s2sIdpToken.customerId = s2sCustomerId;
        e2s2sIdpToken.privateLabelId = String.valueOf(s2sClaim.get("plid"));
        // common fields
        e2s2sIdpToken.jwt = jwt;
        e2s2sIdpToken.claims = claims;
        e2s2sIdpToken.userType = SsoToken.USER_TYPE_SHOPPER;
        e2s2sIdpToken.authType = (String) jsonClaim.get("auth");
        e2s2sIdpToken.delegateUser = shopperDelegateIdpToken;// Add the Delegate Shopper Token
        e2s2sIdpToken.employeeUser = jomaxToken;// Add the Employee Delegate Token
        // always return the Shopper Token
        return e2s2sIdpToken;
    }

    private SsoToken getEmployeeToShopperSsoToken(ReadOnlyJWTClaimsSet claims, SignedJWT jwt) {
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
        jomaxToken.authType = (String) delegateClaim.get("auth");
        jomaxToken.userType = SsoToken.USER_TYPE_EMPLOYEE;
        JSONArray groups = (JSONArray) delegateClaim.get("groups");
        jomaxToken.groups = ssoService.getGroups(jwt);

        // Get the e2s claim
        IdpSsoToken e2sIdpToken = new IdpSsoToken();
        JSONObject e2sClaim = (JSONObject) jsonClaim.get("e2s");
        String e2sType = (String) jsonClaim.get("typ");
        if (e2sType == null || !e2sType.equalsIgnoreCase(e2sIdpToken.getRealm())) {
            logger.warn("Invalid idp JWT: {}", jwt);
            return null;
        }
        String e2sShopperId = (String) e2sClaim.get("shopperId");
        String e2sCustomerId = (String) e2sClaim.get("cid");

        if (e2sShopperId == null) {
            logger.warn("Invalid e2s idp JWT: {}", jwt);
            return null;
        }

        // TYPE_IDP
        e2sIdpToken.shopperId = e2sShopperId;
        e2sIdpToken.customerId = e2sCustomerId;
        e2sIdpToken.privateLabelId = String.valueOf(e2sClaim.get("plid"));
        // common fields
        e2sIdpToken.jwt = jwt;
        e2sIdpToken.claims = claims;
        e2sIdpToken.userType = SsoToken.USER_TYPE_SHOPPER;
        e2sIdpToken.authType = (String) jsonClaim.get("auth");
        e2sIdpToken.employeeUser = jomaxToken;// Add the Employee Delegate Token
        // always return the Shopper Token
        return e2sIdpToken;
    }

    private SsoToken getShopperToShopperSsoToken(ReadOnlyJWTClaimsSet claims, SignedJWT jwt) {
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
        String shopperId = (String) delegateClaim.get("shopperId");
        String customerId = (String) delegateClaim.get("cid");

        if (shopperId == null) {
            logger.warn("Invalid del idp JWT: {}", jwt);
            return null;
        }

        // TYPE_IDP
        idpToken.shopperId = shopperId;
        idpToken.customerId = customerId;
        idpToken.privateLabelId = String.valueOf(delegateClaim.get("plid"));
        // common fields
        idpToken.jwt = jwt;
        idpToken.claims = claims;
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

        String s2sShopperId = (String) s2sClaim.get("shopperId");
        String s2sCustomerId = (String) s2sClaim.get("cid");

        if (s2sShopperId == null) {
            logger.warn("Invalid s2s idp JWT: {}", jwt);
            return null;
        }

        // TYPE_IDP

        s2sIdpToken.shopperId = s2sShopperId;
        s2sIdpToken.customerId = s2sCustomerId;
        s2sIdpToken.privateLabelId = String.valueOf(s2sClaim.get("plid"));
        // common fields
        s2sIdpToken.jwt = jwt;
        s2sIdpToken.claims = claims;
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

    private SsoToken getBasicSsoToken(ReadOnlyJWTClaimsSet claims, SignedJWT jwt) throws ParseException {
        SsoToken ssoToken = null;
        String type = claims.getStringClaim("typ");

        if (type == null) {
            return null;
        }

        if (type.equals("jomax")) {
            // TYPE_JOMAX
            String username = claims.getStringClaim("accountName");
            List<String> groups = ssoService.getGroups(jwt);

            JomaxSsoToken jomaxToken = new JomaxSsoToken();
            jomaxToken.username = username;
            jomaxToken.groups = groups;
            ssoToken = jomaxToken;

        }
        else if (type.equals("idp")) {

            String shopperId = claims.getStringClaim("shopperId");
            String customerId = claims.getStringClaim("cid");

            if (shopperId == null) {
                logger.warn("Invalid IDP JWT: {}", jwt);
                return null;
            }

            // TYPE_IDP
            IdpSsoToken idpToken = new IdpSsoToken();
            idpToken.shopperId = shopperId;
            idpToken.customerId = customerId;
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
        ssoToken.authType = claims.getStringClaim("auth");
        return ssoToken;
    }

    private SsoToken getCertificateToShopperSsoToken(ReadOnlyJWTClaimsSet claims, SignedJWT jwt) {
        Map<String, Object> customClaims = claims.getCustomClaims();
        logger.info("cert2s claims: {}", customClaims);

        JSONObject jsonClaim = claims.toJSONObject();
        JSONObject delegateClaim = (JSONObject) jsonClaim.get("del");
        String type = (String) delegateClaim.get("typ");
        if (!type.equalsIgnoreCase(CertificateToken.REALM)) {
            logger.warn("Invalid del cert2s JWT: {}", jwt);
            return null;
        }

        CertificateToken certToken = new CertificateToken();
        // common fields
        certToken.jwt = jwt;
        try {
            certToken.claims = JWTClaimsSet.parse(delegateClaim);
        } catch (ParseException e) {
            logger.warn("Failed to get Claim Set for cert2s del object");
        }
        certToken.authType = (String) delegateClaim.get("auth");
        certToken.ftc = (long) delegateClaim.get("ftc");
        certToken.iat = (long) delegateClaim.get("iat");
        certToken.jti = (String) delegateClaim.get("jti");
        certToken.o = (String) delegateClaim.get("o");
        certToken.ou = (String) delegateClaim.get("ou");
        certToken.cn = (String) delegateClaim.get("cn");
        JSONObject factors = (JSONObject) delegateClaim.get("factors");
        if(factors != null) {
            certToken.p_cert = (long) factors.get("p_cert");
        }

        // Get the cert2s claim
        JSONObject cert2sClaim = (JSONObject) jsonClaim.get("cert2s");
        String cert2sUsername = (String) cert2sClaim.get("username");
        String cert2sShopperId = (String) cert2sClaim.get("shopperId");
        String cert2sCustomerId = (String) cert2sClaim.get("cid");
        String topLevelType = (String) jsonClaim.get("typ");

        if (cert2sUsername == null || cert2sShopperId == null) {
            logger.warn("Invalid cert2s idp user or shopper null for JWT: {}", jwt);
            return null;
        }

        if(!topLevelType.equalsIgnoreCase(IdpSsoToken.REALM)) {
            logger.warn("Invalid cert2s top typ not valid for idp JWT: {}", jwt);
            return null;
        }

        IdpSsoToken cert2sIdpToken = new IdpSsoToken();
        cert2sIdpToken.shopperId = cert2sShopperId;
        cert2sIdpToken.customerId = cert2sCustomerId;
        cert2sIdpToken.privateLabelId = String.valueOf(cert2sClaim.get("plid"));
        // common fields
        cert2sIdpToken.jwt = jwt;
        cert2sIdpToken.claims = claims;
        cert2sIdpToken.userType = SsoToken.USER_TYPE_SHOPPER;
        cert2sIdpToken.authType = (String) jsonClaim.get("auth");
        cert2sIdpToken.certificateToken = certToken;
        // always return the Shopper Token
        return cert2sIdpToken;
    }
}
