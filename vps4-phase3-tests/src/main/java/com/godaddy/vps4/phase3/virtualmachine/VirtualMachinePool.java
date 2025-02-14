package com.godaddy.vps4.phase3.virtualmachine;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;

public class VirtualMachinePool {

    private static final Logger logger = LoggerFactory.getLogger(VirtualMachinePool.class);

    final int maxTotalVmCount;

    final int maxPerImageVmCount;

    final int maxVmWaitSeconds;

    final Semaphore vmLeases;

    final Map<String, PerImagePool> poolByImageName = new ConcurrentHashMap<>();

    final Set<UUID> claimedCredits = new HashSet<>();

    final Vps4ApiClient apiClient;

    final Vps4ApiClient adminClient;

    final String shopperId;

    final ExecutorService threadPool;

    final int dcId;

    public VirtualMachinePool(int maxTotalVmCount,
                              int maxImageVmCount,
                              int maxVmWaitSeconds,
                              Vps4ApiClient apiClient,
                              Vps4ApiClient adminClient,
                              String shopperId,
                              ExecutorService threadPool,
                              int dcId){

        this.maxTotalVmCount = maxTotalVmCount;
        this.maxPerImageVmCount = maxImageVmCount;
        this.maxVmWaitSeconds = maxVmWaitSeconds;

        this.vmLeases = new Semaphore(maxTotalVmCount);
        this.apiClient = apiClient;
        this.adminClient = adminClient;
        this.shopperId = shopperId;
        this.threadPool = threadPool;
        this.dcId = dcId;
    }

    public VirtualMachine getVm(String imageName){
        PerImagePool perImagePool = poolByImageName.computeIfAbsent(
                imageName,
                key -> new PerImagePool(imageName) );
        return perImagePool.get();
    }


    public void offer(VirtualMachine vm) {
        PerImagePool perImagePool = poolByImageName.get(vm.imageName);
        if (perImagePool != null) {
            perImagePool.offer(vm);
        } else {
            // this _shouldn't_ happen, unless the VM given back to us wasn't
            // checked out from this pool, and we don't have an image of that name
        }
    }

    public void destroyAll(){
        for(PerImagePool pool : poolByImageName.values()){
            pool.destroyAll();
        }
    }

    public void destroy(VirtualMachine vm) {
        PerImagePool perImagePool = poolByImageName.get(vm.imageName);
        if (perImagePool != null) {
            logger.warn("Could not repool vm: " + vm.vmId + ", so Destroying it");
            perImagePool.destroy(vm);
        } else {
            // this _shouldn't_ happen, unless the VM given back to us wasn't
            // checked out from this pool, and we don't have an image of that name
        }
    }

    class PerImagePool {

        final String imageName;
        final BlockingDeque<VirtualMachine> pool;
        final Semaphore perImageLeases;

        public PerImagePool(String imageName) {
            this.imageName = imageName;
            this.pool = new LinkedBlockingDeque<>(VirtualMachinePool.this.maxPerImageVmCount);

            this.perImageLeases = new Semaphore(VirtualMachinePool.this.maxPerImageVmCount);
        }

        public void destroyAll(){
            for(VirtualMachine vm : pool){
                destroy(vm);
            }
        }

        public void destroy(VirtualMachine vm) {

            try {
                // destroy VM
                logger.info("Deleting " + vm.imageName + " -------- " + vm.vmId.toString());

                // TODO Verify the deletions complete.
                apiClient.deleteVm(vm.vmId);
            } finally {
                claimedCredits.remove(vm.orionGuid);
                vmLeases.release();
            }

            // TODO if a VM is destroyed due to an error during a test,
            //      determine if this frees up a lease to provision a
            //      new VM and replenish the pool
            //      (that, if the pool was previously empty, some
            //       other thread may be waiting on pool.takeFirst())
        }

        public void offer(VirtualMachine vm) {
            boolean repooled = pool.offer(vm);
            if (!repooled) {
                logger.debug("pool '{}' at capacity, destroying {}", imageName, vm);
                // pool is already at its capacity, so just destroy the VM
                destroy(vm);
            }
        }

        public VirtualMachine get() {
            // if we have a VM available, return it

            // if we don't have a VM, but are under our per-image limit and
            // max-vm limit, create one

            // if we don't have a VM, and we can't create one, wait for a VM to
            // be returned to the pool
            VirtualMachine vm = pool.pollFirst();

            if (vm == null) {
                // no pooled VMs, can we spin one up?
                if (perImageLeases.tryAcquire()) {
                    if (vmLeases.tryAcquire()) {
                        createVm();
                    } else {
                        perImageLeases.release();
                    }
                }
                // we _can't_ spin one up, so we have to wait until one is returned
                // to the pool
                logger.debug("waiting for vm with image {}", imageName);
                try {
                    vm = pool.pollFirst(maxVmWaitSeconds, TimeUnit.SECONDS);
                    if (vm == null) {

                        logger.error("Can't wait forever, giving up! Never got vm with image " + imageName);
                        throw new RuntimeException("CreateVm Timed out. " + imageName + " vm did not complete.");
                    }
                    if(!vm.createSuccess) {
                        logger.error("VM Creation failed, giving up! Vm {} failed to create", vm.vmId);
                        throw new RuntimeException("CreateVm Failed for " + imageName + " with vmId " + vm.vmId);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            logger.debug("acquired {}", vm);
            return vm;
        }

        public void createVm() {
            threadPool.submit(() -> {
                UUID orionGuid = getAndClaimVmCredit();
                if (orionGuid == null) {
                    vmLeases.release();
                    logger.error("No credit available to create vm {}", imageName);
                }
                else {
                    // Create the VM and add to the pool
                    logger.debug("creating vm for {}, using credit guid {}", imageName, orionGuid);
                    ProvisionVmResult result = provisionVm(orionGuid);
                    VirtualMachine vm = new VirtualMachine(VirtualMachinePool.this, VirtualMachinePool.this.apiClient,
                                                           VirtualMachinePool.this.adminClient, this.imageName,
                                                           username, password, result.vmId, orionGuid, result.success);
                    if (vm.isWindows()) {
                        enableWinexe(vm.vmId);
                    }
                    offer(vm);
            }});
        }

        public UUID getAndClaimVmCredit() {
            // get credit
            // return if null
            // try to claim
            // return if claimed
            // repeat
            synchronized (claimedCredits) {
                UUID credit = getVmCredit();
                while (credit != null) {
                    if (canClaimCredit(credit)) {
                        logger.info("Successfully claimed credit: {}", credit);
                        return credit;
                    }
                    logger.debug("Another test already claimed the credit {}, will try again", credit);
                    try {
                        Thread.sleep(1000); // wait a few seconds before trying again, coz the api takes a bit
                    } catch (InterruptedException e) {
                        logger.error("Waiting for new credit interrupted", e);
                    }
                    credit = getVmCredit();
                }
            }

            logger.info("Other tests have claimed all available credits for image : " + imageName);
            return null;
        }

        public UUID getVmCredit() {
            JSONObject image = apiClient.getImage(imageName);
            if (image.get("imageId") == null) {
                logger.error("Invalid image: " + imageName);
                throw new RuntimeException("Invalid image: " + imageName);
            }
            String os = image.get("operatingSystem").toString();
            String controlPanel = image.get("controlPanel").toString();
            JSONObject serverType = (JSONObject) image.get("serverType");
            String platform = serverType.get("platform").toString();;

            UUID vmCredit = apiClient.getVmCredit(shopperId,  os,  controlPanel, platform);
            if (vmCredit == null && claimedCredits.isEmpty()) {
                logger.error("There are no credits available to run tests for image : {}", imageName);
                throw new RuntimeException();
            }

            return vmCredit;
        }

        public synchronized boolean canClaimCredit(UUID vmCredit) {
            return vmCredit != null && claimedCredits.add(vmCredit);
        }

        static final String username = "vpstester";
        static final String password = "thisvps4TEST!";

        public ProvisionVmResult provisionVm(UUID orionGuid){
            ProvisionVmResult result = new ProvisionVmResult();
            result.success = false;
            JSONObject provisionResult = apiClient.provisionVm("VPS4 Phase 3 Test VM",
                                                               orionGuid,
                                                               imageName,
                                                               dcId,
                                                               username,
                                                               password);
            logger.debug("provision vm: {}", provisionResult.toJSONString());
            result.vmId = UUID.fromString(provisionResult.get("virtualMachineId").toString());
            long actionId = Long.parseLong(provisionResult.get("id").toString());
            logger.debug("Creating vmId {} for orionGuid {} with actionId {}", result.vmId, orionGuid, actionId);
            try{
                apiClient.pollForVmActionComplete(result.vmId, actionId, maxVmWaitSeconds);
                result.success = true;
            }
            catch (RuntimeException ex)
            {
                logger.error("Failed to provisionVm {} for phase3 test", result.vmId);
            }
            return result;
        }

        public void enableWinexe(UUID vmId) {
            long actionId = adminClient.enableWinexe(vmId);
            logger.debug("Opening winexe ports for vmId {} with actionId {}", vmId, actionId);
            apiClient.pollForVmActionComplete(vmId, actionId, maxVmWaitSeconds);
        }
    }

}
