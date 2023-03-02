package com.godaddy.vps4.sso.clients;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * This annotation indicates that a given SSO service is to be used with the Messaging API.
 */
@BindingAnnotation
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Vps4SsoMessagingApi {}
