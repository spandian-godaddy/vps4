package com.godaddy.vps4.client;

@FunctionalInterface
public interface WithShopperId<T> {
    T execute();
}
