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

        String xCertHeader = request.getHeader("X-Cert-Subject-DN");
        if(xCertHeader == null) {
            return false;
        }

        // nginx validated a cert auth and has sent us this header.
        // authentication succeeds if the cert's CN matches the one in our configuration
        String cnField = getCNFieldFromHeader(xCertHeader);
        return cnField != null && (authenticatesDevCert(cnField) || authenticatesSchedulerCert(cnField));
    }

    private boolean authenticatesSchedulerCert(String cnField){
        boolean result = cnField.equals(config.get("vps4.api.schedulerCertCN"));
        if(result){
            logger.debug("Validated scheduler cert");
        }
        return result;
    }

    private boolean authenticatesDevCert(String cnField){
        boolean result = cnField.equals(config.get("vps4.api.developerCertCN"));
        if(result){
            logger.debug("Validated developer cert");
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
