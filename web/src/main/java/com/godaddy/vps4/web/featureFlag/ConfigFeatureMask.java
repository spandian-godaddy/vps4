package com.godaddy.vps4.web.featureFlag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConfigFeatureMask {
    Class<? extends FeatureSetting> setting();
    boolean disabled() default true;
}
