package com.godaddy.vps4.web.featureFlag;


import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

public class ConfigFeatureMaskModule extends AbstractModule {
    protected void configure() {
        ConfigFeatureMaskInterceptor configFeatureMaskInterceptor = new ConfigFeatureMaskInterceptor();
        requestInjection(configFeatureMaskInterceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(ConfigFeatureMask.class), configFeatureMaskInterceptor);

        // Classes that represent the setting go here
        bind(ImageListFeatureSetting.class);
        bind(ImageDetailFeatureSetting.class);
    }
}
