package com.godaddy.vps4.cpanel;

import com.godaddy.hfs.cpanel.CPanelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;


public class CachedCpanelApiTokenService implements CpanelApiTokenService {

    static final Logger logger = LoggerFactory.getLogger(CachedCpanelApiTokenService.class);

    final ConcurrentHashMap<Long, CachedApiToken> cache = new ConcurrentHashMap<>();

    final ExecutorService threadPool;

    final CpanelApiTokenService apiTokenService;

    public CachedCpanelApiTokenService(ExecutorService threadPool, CpanelApiTokenService apiTokenService) {
        this.threadPool = threadPool;
        this.apiTokenService = apiTokenService;
    }

    public CachedCpanelApiTokenService(ExecutorService threadPool, CPanelService cPanelService) {
        this(threadPool, new HfsCpanelApiTokenService(cPanelService));
    }

    @Override
    public void invalidateApiToken(long vmId, String apiToken) {
        CachedApiToken cached = cache.get(vmId);
        if (cached != null && cached.getApiToken().equals(apiToken)) {
            cached.invalidate(cached.apiToken);
        }

        apiTokenService.invalidateApiToken(vmId, apiToken);
    }

    @Override
    public String getApiToken(long vmId, Instant timeoutAt) {

        CachedApiToken cached = cache.get(vmId);
        if (cached == null) {
            // no cached api token for this VM
            cached = new CachedApiToken(vmId);
            CachedApiToken existing = cache.putIfAbsent(vmId, cached);
            if (existing != null) {
                logger.info("cached entry for vm {} was created while we were waiting, using it", vmId);
                cached = existing;
            }
        }

        // is the cached value good?
        if (Instant.now().isBefore(cached.expiresAt)) {
            logger.info("api token cache hit for vm {}", vmId);
            // Note: this may be null if the cached value represents "we couldn't retrieve the hash"
            return cached.getApiToken();
        }

        // the cached value needs to be refreshed
        // see if we need to do it, or if somebody else is already doing it
        if (cached.fetching.compareAndSet(false, true)) {

            logger.info("spinning off api token fetch for vm {}", vmId);

            // we own fetching, spin off a thread
            Fetcher fetcher = new Fetcher(
                    () -> apiTokenService.getApiToken(vmId, timeoutAt),
                    cached);

            threadPool.submit(fetcher);
        }

        // at this point, _someone_ is fetching the api token, so
        // wait on the signal when the fetch is complete
        final Object fetchDoneSignal = cached.lock;

        logger.info("api token fetch in progress for vm {}, waiting...", vmId);

        // if we're still fetching,
        // (i.e. it didn't complete while we were in lock acquisition)
        // then wait the fetchDone condition
        while (cached.fetching.get()
                && Instant.now().isBefore(timeoutAt)) {

            synchronized(fetchDoneSignal) {
                try {
                    fetchDoneSignal.wait(200);
                }
                catch (InterruptedException e) {

                }
            }
        }

        // if we now have a good value, return that
        if (Instant.now().isBefore(cached.expiresAt)) {
            logger.info("fetch successful for vm {}", vmId);
            return cached.getApiToken();
        }

        // if the the current cached value is still expired,
        // and we (probably) haven't been able to get a new one,
        // don't just return the current expired api token,
        // return null to indicate an issue
        logger.info("waited for api token for vm {}, "
                + "but fetch didn't complete before our timeout", vmId);
        return null;
    }

    static class Fetcher implements Runnable {

        final Callable<String> callable;

        final CachedApiToken cached;

        public Fetcher(Callable<String> callable, CachedApiToken cached) {
            this.callable = callable;
            this.cached = cached;
        }

        @Override
        public void run() {
            try {
                String apiToken = callable.call();

                // update the cache with the successful fetch result
                cached.apiToken = apiToken;
                cached.expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

            } catch (Exception e) {
                logger.error("Error getting api token from vm " + cached.vmId, e);

                // update the cache with the failure fetch result
                // (expiresAt is much sooner than with success)
                cached.apiToken = null;
                cached.expiresAt = Instant.now().plus(30, ChronoUnit.SECONDS);

            } finally {
                // signal to everyone waiting for the fetch that it's finished
                final Object fetchDoneSignal = cached.lock;
                synchronized(fetchDoneSignal) {
                    cached.fetching.set(false);
                    fetchDoneSignal.notifyAll();
                }
            }
        }

    }

    public static class CachedApiToken {

        private final long vmId;

        private volatile String apiToken;

        private volatile Instant fetchedAt;

        private volatile Instant expiresAt = Instant.now();

        private final Object lock = new Object();

        private final AtomicBoolean fetching = new AtomicBoolean();

        public CachedApiToken(long vmId) {
            this.vmId = vmId;
        }

        public long getVmId() {
            return vmId;
        }

        public Instant getFetchedAt() {
            return fetchedAt;
        }

        public String getApiToken() {
            return apiToken;
        }

        /**
         * invalidate the cache at the current 'fetchedAt' time
         * @param apiToken
         */
        public void invalidate(String apiToken) {
            synchronized(lock) {
                if (!fetching.get()) {
                    if (Objects.equals(apiToken, this.apiToken)) {
                        apiToken = null;
                        fetchedAt = null;
                        expiresAt = Instant.now();
                    }
                }
            }
        }

    }

}
