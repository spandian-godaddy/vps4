package com.godaddy.vps4.handler.util;

import org.apache.http.conn.HttpHostConnectException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServiceUnavailableException;


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
            || ex instanceof InternalServerErrorException // Orch engine is down but api is up, comes back as a 500
            || ex instanceof ServiceUnavailableException;  // Api returns a 503
    }
}
