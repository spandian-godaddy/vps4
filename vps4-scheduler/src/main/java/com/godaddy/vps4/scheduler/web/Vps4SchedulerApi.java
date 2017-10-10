package com.godaddy.vps4.scheduler.web;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to indicate a given type represents a VPS4 Scheduler API.
 *
 * This is to differentiate a class that happens to be present in a
 * collection that may have the JAX-RS annotations present, but is
 * not part of the VPS4 scheduler API (for instance, an internal interface
 * VPS4 scheduler consumes)
 *
 */
@BindingAnnotation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Vps4SchedulerApi {

}
