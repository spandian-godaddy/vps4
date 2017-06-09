package com.godaddy.vps4.util;

/**
 *
 * Tracks a 'request ID' in a ThreadLocal.
 *
 * General usage pattern is:
 *
 * <pre>
 *     set(UUID);
 *     try {
 *         // makeInnerCall();
 *
 *     } finally {
 *         set(null);
 *     }
 * </pre>
 *
 * {@link #get()} can be called within that call stack to fetch the request UUID.
 *
 *
 */
public class ThreadLocalRequestId {

    private static final ThreadLocal<String> threadLocalRequestId = new ThreadLocal<>();

    public static void set(String requestId) {
        threadLocalRequestId.set(requestId);
    }

    public static String get() {
        return threadLocalRequestId.get();
    }
}
