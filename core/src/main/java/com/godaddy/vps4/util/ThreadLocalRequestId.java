package com.godaddy.vps4.util;

import java.util.UUID;

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

    private static final ThreadLocal<UUID> threadLocalRequestId = new ThreadLocal<>();

    public static void set(UUID requestId) {
        threadLocalRequestId.set(requestId);
    }

    public static UUID get() {
        return threadLocalRequestId.get();
    }
}
