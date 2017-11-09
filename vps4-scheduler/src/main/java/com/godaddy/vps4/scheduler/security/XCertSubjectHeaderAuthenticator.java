package com.godaddy.vps4.scheduler.security;

import com.godaddy.hfs.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class XCertSubjectHeaderAuthenticator implements RequestAuthenticator<Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(XCertSubjectHeaderAuthenticator.class);

    private final Config config;

    @Inject
    public XCertSubjectHeaderAuthenticator(Config config) {
        this.config = config;
    }

    @Override
    public Boolean authenticate(HttpServletRequest request) {

        Boolean result = false;

        String xCertHeader = request.getHeader("X-Cert-Subject-DN");
        if (xCertHeader != null) {
            String cnField = getCNFieldFromHeader(xCertHeader);

            // nginx validated a cert auth and has sent us this header.
            // authentication succeeds the cert's CN matches the one in configuration
            result = cnField != null && cnField.equals(config.get("vps4.api.certCN"));
        }
        return result;
    }

    private String getCNFieldFromHeader(String header){
        try {
            List<String> dnFields = Arrays.asList(header.split(","));
            List<String> cnFieldList = dnFields.stream().filter(field -> field.startsWith("CN=")).
                    collect(Collectors.toList());
            return cnFieldList.get(0).split("=")[1];
        }catch (Exception e){
            logger.warn("Exception while parsing header {}", e);
            return null;
        }
    }
}
