package com.godaddy.vps4.cpanel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

public class CachedCpanelAccessHashServiceTest {

    @Test
    public void testCachedAccessHash() throws Exception {

        // wire the underlying access hash service the caching layer is hitting
        CpanelAccessHashService mockService = mock(CpanelAccessHashService.class);
        when(mockService.getAccessHash(eq(7L), anyString(), anyString(), any(Instant.class)))
            .thenReturn("accesshash7");

        when(mockService.getAccessHash(eq(8L), anyString(), anyString(), any(Instant.class)))
            .thenReturn("accesshash8");

        ExecutorService fetchPool = Executors.newCachedThreadPool();
        CachedCpanelAccessHashService accessHashService = new CachedCpanelAccessHashService(
                fetchPool, mockService);

        ExecutorService workerPool = Executors.newCachedThreadPool();

        // launch threads requesting vmId=7
        List<Future<String>> vm7Futures = new ArrayList<>();
        for (int i=0; i<4; i++) {
            vm7Futures.add(workerPool.submit(
                    () -> accessHashService.getAccessHash(
                        7, "1.2.3.4", "0.0.0.0", Instant.now().plusSeconds(10))));
        }

        // launch threads requesting vmId=8
        List<Future<String>> vm8Futures = new ArrayList<>();
        for (int i=0; i<4; i++) {
            vm8Futures.add(workerPool.submit(
                    () -> accessHashService.getAccessHash(
                        8, "1.2.3.4", "0.0.0.0", Instant.now().plusSeconds(10))));
        }

        workerPool.shutdown();
        workerPool.awaitTermination(2, TimeUnit.SECONDS);

        // the underlying access hash services should have only been
        // called once for each vmId
        verify(mockService, atMost(1))
                .getAccessHash(eq(7), anyString(), anyString(), any(Instant.class));

        verify(mockService, atMost(1))
                .getAccessHash(eq(8), anyString(), anyString(), any(Instant.class));

        // ensure all requestors got their respective access hashes
        for (Future<String> vm7Future : vm7Futures) {
            assertEquals("accesshash7", vm7Future.get());
        }
        for (Future<String> vm8Future : vm8Futures) {
            assertEquals("accesshash8", vm8Future.get());
        }
    }

    // TODO cached expiration and re-fetch on expired access hash
    public void testExpiredAccessHash() throws Exception {

    }

    // TODO cached expiration and re-fetch on invalidated access hash
    public void testInvalidatedAccessHash() throws Exception {

    }
}
