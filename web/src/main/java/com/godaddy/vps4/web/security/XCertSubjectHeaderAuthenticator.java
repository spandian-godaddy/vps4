package com.godaddy.vps4.web.security;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;

public class XCertSubjectHeaderAuthenticator implements RequestAuthenticator<GDUser> {

    private static final Logger logger = LoggerFactory.getLogger(XCertSubjectHeaderAuthenticator.class);

    private final Config config;

    @Inject
    public XCertSubjectHeaderAuthenticator(Config config) {
        this.config = config;
    }

    @Override
    public GDUser authenticate(HttpServletRequest request) {

        GDUser gdUser = null;

        String xCertHeader = request.getHeader("X-Cert-Subject-DN");
        if (xCertHeader != null) {
            String cnField = getCNFieldFromHeader(xCertHeader);

            if(validateCNFieldFromScheduler(cnField) || validateCNFieldFromDeveloper(cnField)) {
                // nginx validated a cert auth from the scheduler or developer and has sent us this header.
                // authentication succeeds if the user can be created correctly
                gdUser = createGDUser(request);
                if (gdUser.isShopper()) {
                    logger.info("GD User authenticated: {}, URI: {}", gdUser.toString(), request.getRequestURI());
                }
            }
        }
        return gdUser;
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

    private boolean validateCNFieldFromScheduler(String cnField){
        return cnField != null && cnField.equals(config.get("vps4.scheduler.certCN"));
    }

    private boolean validateCNFieldFromDeveloper(String cnField){
        return cnField != null && cnField.equals(config.get("vps4.developer.certCN"));
    }

    private GDUser createGDUser(HttpServletRequest request) {
        String shopperId = request.getHeader("X-Shopper-Id");

        GDUser gdUser = new GDUser();
        gdUser.shopperId = shopperId;
        gdUser.isEmployee = true;
        gdUser.isStaff = true;
        gdUser.isAdmin = false;
        return gdUser;
    }
}
