package com.godaddy.vps4.web.featureFlag;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;


public class ImageDetailFeatureSetting extends ImageFeatureSetting {
    private static final Logger logger = LoggerFactory.getLogger(ImageDetailFeatureSetting.class);

    @Inject
    public ImageDetailFeatureSetting(Config config, GDUser gdUser) {
        super(config, gdUser);
    }

    @Override
    public Object handle(Object o) {
        logger.debug(String.format("In handle %s", this.getClass().getName()));
        if (o instanceof Image) {
            Image image = (Image) o;

            if (image != null && isImageAvailableInEnvironment(image) && isImageAvailableForRole(image)) {
                return image;
            }

            throw new NotFoundException("Unknown image");
        }

        return o;
    }
}
