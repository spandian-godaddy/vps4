package com.godaddy.vps4.web.featureFlag;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


abstract public class ImageFeatureSetting implements FeatureSetting {
    public static final String FEATUREDFLAG_IMAGES_ENV_DISABLED = "featuredflag.images.env.disabled";
    public static final String FEATUREDFLAG_IMAGES_CUSTOMER_DISABLED = "featuredflag.images.role.disabled";

    private GDUser gdUser;
    private Config config;
    private Set<String> disabledImagesInCurrEnv;
    private Set<String> disabledImagesForCustomers;

    @Inject
    public ImageFeatureSetting(Config config, GDUser gdUser) {
        this.config = config;
        this.gdUser = gdUser;
        disabledImagesInCurrEnv = new HashSet<>(
            Arrays.asList(this.config.get(FEATUREDFLAG_IMAGES_ENV_DISABLED, "").split(",")));
        disabledImagesForCustomers = new HashSet<>(
                Arrays.asList(this.config.get(FEATUREDFLAG_IMAGES_CUSTOMER_DISABLED, "").split(",")));
    }

    boolean isImageAvailableInEnvironment(Image image) {
        return (!disabledImagesInCurrEnv.contains(image.hfsName));
    }

    boolean isImageAvailableForRole(Image image) {
        // For now only customers have images optionally disabled for them
        // but this could be expanded to provide more fine grained per role image filtering
        return (!(gdUser.isPayingCustomer()) || !disabledImagesForCustomers.contains(image.hfsName));
    }
}
