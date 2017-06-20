package com.godaddy.vps4.web.util;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.Environment;

public class AlphaHelper {

    private final Logger logger = LoggerFactory.getLogger(AlphaHelper.class);

    private final Config config;

    @Inject
    public AlphaHelper(Config config) {
        this.config = config;
    }

    private boolean isStagingOrProductionEnv() {
        return ((Environment.CURRENT == Environment.STAGE) || (Environment.CURRENT == Environment.PROD) ? true : false);
    }

    private boolean isInternalShopper(String shopperId) {
        return (shopperId.length() == 3);
    }

    public void verifyValidAlphaUser(String shopperId) {
        // TODO: Remove this after ECOMM integration
        boolean allow3LetterAccountsOnly = Boolean.parseBoolean(config.get("allow3LetterAccountsOnly", "true"));
        logger.info("Environment Staging or Production? : {}", isStagingOrProductionEnv());
        logger.info("Allow internal shoppers only: {}", allow3LetterAccountsOnly );
        if (isStagingOrProductionEnv() && allow3LetterAccountsOnly && !isInternalShopper(shopperId)) {
            logger.warn("Non-3-letter shopper encountered in ALPHA release: {}", shopperId);
            throw new RuntimeException("Currently only 3 letter accounts are allowed in ALPHA release. ");
        }
    }
}
