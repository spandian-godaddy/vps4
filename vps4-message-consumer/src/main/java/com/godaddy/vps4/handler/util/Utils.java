package com.godaddy.vps4.handler.util;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

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

    public static boolean isVps4ApiDown(Throwable ex) {
        return  (ex instanceof ProcessingException && ex.getCause() instanceof HttpHostConnectException) // If the api is down
            || ex instanceof WebApplicationException; //  4xx or 5xx errors from api or orch engine
    }
}
