package com.godaddy.vps4.client;

/**
 *
 * Tracks a 'shopper ID' in a ThreadLocal.
 *
 * General usage pattern is:
 *
 * <pre>
 *     set(shopperId);
 *     try {
 *         // makeInnerCall();
 *
 *     } finally {
 *         set(null);
 *     }
 * </pre>
 *
 * {@link #get()} can be called within that call stack to fetch the shopper Id.
 *
 *
 */
public class ThreadLocalShopperId {

    private static final ThreadLocal<String> threadLocalShopperId = new ThreadLocal<>();

    public static void set(String shopperId) {
        threadLocalShopperId.set(shopperId);
    }

    public static String get() {
        return threadLocalShopperId.get();
    }
}
