package com.godaddy.vps4.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * An annotation to indicate a given type represents a public VPS4 API.
 *
 * This is to differentiate a class that happens to be present in a
 * collection that may have the JAX-RS annotations present, but is
 * not part of the public VPS4 API (for instance, an internal interface
 * VPS4 consumes)
 *
 */
@BindingAnnotation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Vps4Api {

}
