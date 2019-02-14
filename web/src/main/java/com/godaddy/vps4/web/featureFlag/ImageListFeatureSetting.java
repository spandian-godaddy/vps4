package com.godaddy.vps4.web.featureFlag;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;


public class ImageListFeatureSetting extends ImageFeatureSetting {
    private static final Logger logger = LoggerFactory.getLogger(ImageListFeatureSetting.class);

    @Inject
    public ImageListFeatureSetting(Config config, GDUser gdUser) {
        super(config, gdUser);
    }

    @Override
    public Object handle(Object o) {
        logger.debug(String.format("In handle %s", this.getClass().getName()));
        if (o instanceof Set) {
            Set<Image> images = (Set<Image>) o;
            images = images.stream()
                    .filter(this::isImageAvailableInEnvironment)
                    .filter(this::isImageAvailableForRole)
                    .collect(Collectors.toSet());
            return images;
        }

        return o;
    }
}
