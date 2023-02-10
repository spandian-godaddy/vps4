package com.godaddy.hfs.sso.token;

public class CertificateToken extends SsoToken{

    public static final String REALM = "cert";
    public long ftc;
    public long iat;
    public String jti;
    public String o;
    public String ou;
    public String cn;
    public long p_cert;
    
    @Override
    public String getRealm() {
        return REALM;
    }
    
}
