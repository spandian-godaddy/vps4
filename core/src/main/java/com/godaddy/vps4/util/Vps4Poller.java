package com.godaddy.vps4.util;

@FunctionalInterface
public interface Vps4Poller<T, U, R> {
    R poll(T t, U u) throws PollerTimedOutException;
}
