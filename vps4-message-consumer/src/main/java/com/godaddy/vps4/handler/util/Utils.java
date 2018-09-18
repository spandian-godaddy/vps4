package com.godaddy.vps4.handler.util;

import org.apache.http.conn.HttpHostConnectException;

public abstract class Utils {
    public static boolean isOrchEngineDown(Throwable ex) {
        return ex instanceof RuntimeException
                && (ex.getCause() instanceof HttpHostConnectException // unclustered orch-engine down error
                || ex.getMessage().equals("Timed out attempting to contact cluster")); // Clustered orch-engine down error
    }

    public static boolean isDBError(Throwable ex) {
        return  ex instanceof RuntimeException
                &&  ex.getMessage().startsWith("Sql."); // All sql related exception start "Sql."
    }
}
