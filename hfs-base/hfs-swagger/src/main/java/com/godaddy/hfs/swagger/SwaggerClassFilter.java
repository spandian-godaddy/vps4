package com.godaddy.hfs.swagger;

import java.util.function.Predicate;

@FunctionalInterface
public interface SwaggerClassFilter extends Predicate<Class<?>> {


}
