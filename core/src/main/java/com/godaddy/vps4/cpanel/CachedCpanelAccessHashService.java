package com.godaddy.vps4.cpanel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.godaddy.hfs.cpanel.CPanelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CachedCpanelAccessHashService implements CpanelAccessHashService {

    static final Logger logger = LoggerFactory.getLogger(CachedCpanelAccessHashService.class);

    final ConcurrentHashMap<Long, CachedAccessHash> cache = new ConcurrentHashMap<>();

    final ExecutorService threadPool;

    final CpanelAccessHashService accessHashService;

    public CachedCpanelAccessHashService(ExecutorService threadPool, CpanelAccessHashService accessHashService) {
        this.threadPool = threadPool;
        this.accessHashService = accessHashService;
    }

    public CachedCpanelAccessHashService(ExecutorService threadPool, CPanelService cPanelService) {
        this(threadPool, new HfsCpanelAccessHashService(cPanelService));
    }

    @Override
    public void invalidAccessHash(long vmId, String accessHash) {
        CachedAccessHash cached = cache.get(vmId);
        if (cached != null && cached.getAccessHash().equals(accessHash)) {
            cached.invalidate(cached.accessHash);
        }

        accessHashService.invalidAccessHash(vmId, accessHash);
    }

    @Override
    public String getAccessHash(long vmId, String publicIp, Instant timeoutAt) {

        CachedAccessHash cached = cache.get(vmId);
        if (cached == null) {
            // no cached access hash for this VM
            cached = new CachedAccessHash(vmId);
            CachedAccessHash existing = cache.putIfAbsent(vmId, cached);
            if (existing != null) {
                logger.trace("cached entry for vm {} was created while we were waiting, using it", vmId);
                cached = existing;
            }
        }

        // is the cached value good?
        if (Instant.now().isBefore(cached.expiresAt)) {
            logger.debug("access hash cache hit for vm {}", vmId);
            // Note: this may be null if the cached value represents "we couldn't retrieve the hash"
            return cached.getAccessHash();
        }

        // the cached value needs to be refreshed
        // see if we need to do it, or if somebody else is already doing it
        if (cached.fetching.compareAndSet(false, true)) {

            logger.debug("spinning off access hash fetch for vm {}", vmId);

            // we own fetching, spin off a thread
            Fetcher fetcher = new Fetcher(
                    () -> accessHashService.getAccessHash(vmId, publicIp, timeoutAt),
                    cached);

            threadPool.submit(fetcher);
        }

        // at this point, _someone_ is fetching the access hash, so
        // wait on the signal when the fetch is complete
        final Object fetchDoneSignal = cached.lock;

        logger.debug("access hash fetch in progress for vm {}, waiting...", vmId);

        // if we're still fetching,
        // (i.e. it didn't complete while we were in lock acquisition)
        // then wait the fetchDone condition
        while (cached.fetching.get()
                && Instant.now().isBefore(timeoutAt)) {

            synchronized(fetchDoneSignal) {
                try {
                    fetchDoneSignal.wait(200);
                    // FIXME while loop around wait, factor in 'timeoutAt'
                }
                catch (InterruptedException e) {

                }
            }
        }

        // if we now have a good value, return that
        if (Instant.now().isBefore(cached.expiresAt)) {
            logger.debug("fetch successful for vm {}", vmId);
            return cached.getAccessHash();
        }

        // if the the current cached value is still expired,
        // and we (probably) haven't been able to get a new one,
        // don't just return the current expired access hash,
        // return null to indicate an issue
        logger.debug("waited for access hash for vm {}, "
                + "but fetch didn't complete before our timeout", vmId);
        return null;
    }

    static class Fetcher implements Runnable {

        final Callable<String> callable;

        final CachedAccessHash cached;

        public Fetcher(Callable<String> callable, CachedAccessHash cached) {
            this.callable = callable;
            this.cached = cached;
        }

        @Override
        public void run() {
            try {
                String accessHash = callable.call();

                // update the cache with the successful fetch result
                cached.accessHash = accessHash;
                cached.expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

            } catch (Exception e) {
                logger.error("Error getting access hash from vm " + cached.vmId, e);

                // update the cache with the failure fetch result
                // (expiresAt is much sooner than with success)
                cached.accessHash = null;
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


    public static class CachedAccessHash {

        private final long vmId;

        private volatile String accessHash;

        private volatile Instant fetchedAt;

        private volatile Instant expiresAt = Instant.now();

        private final Object lock = new Object();

        private final AtomicBoolean fetching = new AtomicBoolean();

        public CachedAccessHash(long vmId) {
            this.vmId = vmId;
        }

        public long getVmId() {
            return vmId;
        }

        public Instant getFetchedAt() {
            return fetchedAt;
        }

        public String getAccessHash() {
            return accessHash;
        }

        /**
         * invalidate the cache at the current 'fetchedAt' time
         * @param accessHash
         */
        public void invalidate(String accessHash) {
            synchronized(lock) {
                if (!fetching.get()) {
                    if (Objects.equals(accessHash, this.accessHash)) {
                        accessHash = null;
                        fetchedAt = null;
                        expiresAt = Instant.now();  // TODO should we cache the fact that this key didn't work?
                    }
                }
            }
        }

    }

}
