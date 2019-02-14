package com.godaddy.vps4.web.featureFlag;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;


public class ConfigFeatureMaskInterceptor implements MethodInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFeatureMaskInterceptor.class);

    @Inject Injector injector;

    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method resourceMethod = invocation.getMethod();
        ConfigFeatureMask configFeatureMask = resourceMethod.getAnnotation(ConfigFeatureMask.class);

        Object o = invocation.proceed();

        if (configFeatureMask != null && !configFeatureMask.disabled()) {
            Class<? extends FeatureSetting> featureSettingCls = configFeatureMask.setting();
            logger.debug(
                String.format("In guice method interceptor: %s, forwarding response to ConfigFeatureMask setting: %s",
                this.getClass().getName(), featureSettingCls.getName())
            );

            FeatureSetting featureSetting = injector.getInstance(featureSettingCls);
            return featureSetting.handle(o);
        }

        return  o;
    }

}
