package com.godaddy.vps4.handler.util;

import org.apache.http.conn.HttpHostConnectException;
import org.junit.Test;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServiceUnavailableException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;


public class UtilsTest {
    @Test
    public void isOrchEngineDownReturnsWhenErrorConnectingToUnclusteredInstance() throws Exception {
        assertTrue(Utils.isOrchEngineDown(new RuntimeException(mock(HttpHostConnectException.class))));
    }

    @Test
    public void isOrchEngineDownReturnsWhenErrorConnectingToClusteredInstance() throws Exception {
        assertTrue(Utils.isOrchEngineDown(new RuntimeException("Timed out attempting to contact cluster")));
    }

    @Test
    public void isDBErrorReturnsTrueIfRuntimeException() throws Exception {
        assertTrue(Utils.isDBError(new RuntimeException("Sql.blah")));
    }

    @Test
    public void isDBErrorReturnsFalse() throws Exception {
        assertFalse(Utils.isDBError(new RuntimeException("FOObar")));
    }

    @Test
    public void isVps4ApiDownReturnsTrueIfOrchEngineDown() throws Exception {
        assertTrue(Utils.isVps4ApiDown(mock(InternalServerErrorException.class)));
    }

    @Test
    public void isVps4ApiDownReturnsTrueIfVps4ServiceDown() throws Exception {
        assertTrue(Utils.isVps4ApiDown(mock(ServiceUnavailableException.class)));
    }

    @Test
    public void isVps4ApiDownReturnsTrueIfHttpSessionCantBeEstablished() throws Exception {
        assertTrue(Utils.isVps4ApiDown(new ProcessingException(mock(HttpHostConnectException.class))));
    }

}