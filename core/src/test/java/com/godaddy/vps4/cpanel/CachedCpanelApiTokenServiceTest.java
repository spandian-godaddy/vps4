package com.godaddy.vps4.cpanel;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachedCpanelApiTokenServiceTest {
    @Test
    public void testCachedApiToken() throws Exception {
        // wire the underlying access api token the caching layer is hitting
        CpanelApiTokenService mockService = mock(CpanelApiTokenService.class);
        when(mockService.getApiToken(eq(7L), any(Instant.class)))
            .thenReturn("apitoken7");

        when(mockService.getApiToken(eq(8L), any(Instant.class)))
            .thenReturn("apitoken8");

        ExecutorService fetchPool = Executors.newCachedThreadPool();
        CachedCpanelApiTokenService apiTokenService = new CachedCpanelApiTokenService(
                fetchPool, mockService);

        ExecutorService workerPool = Executors.newCachedThreadPool();

        // launch threads requesting vmId=7
        List<Future<String>> vm7Futures = new ArrayList<>();
        for (int i=0; i<4; i++) {
            vm7Futures.add(workerPool.submit(
                    () -> apiTokenService.getApiToken(
                        7, Instant.now().plusSeconds(10))));
        }

        // launch threads requesting vmId=8
        List<Future<String>> vm8Futures = new ArrayList<>();
        for (int i=0; i<4; i++) {
            vm8Futures.add(workerPool.submit(
                    () -> apiTokenService.getApiToken(
                        8, Instant.now().plusSeconds(10))));
        }

        workerPool.shutdown();
        workerPool.awaitTermination(2, TimeUnit.SECONDS);

        // the underlying api token services should have only been
        // called once for each vmId
        verify(mockService, atMost(1))
                .getApiToken(eq(7), any(Instant.class));

        verify(mockService, atMost(1))
                .getApiToken(eq(8), any(Instant.class));

        // ensure all requestors got their respective api tokens
        for (Future<String> vm7Future : vm7Futures) {
            assertEquals("apitoken7", vm7Future.get());
        }
        for (Future<String> vm8Future : vm8Futures) {
            assertEquals("apitoken8", vm8Future.get());
        }
    }

    // TODO cached expiration and re-fetch on expired api token
    public void testExpiredApiToken() throws Exception {

    }

    // TODO cached expiration and re-fetch on invalidated api token
    public void testInvalidatedApiToken() throws Exception {

    }
}
