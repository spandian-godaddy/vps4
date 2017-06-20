package com.godaddy.vps4.phase3.virtualmachine;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    final Vps4ApiClient apiClient;

    final Vps4ApiClient adminClient;

    final String shopperId;

    final ExecutorService threadPool;

    public VirtualMachinePool(int maxTotalVmCount, int maxImageVmCount, int maxVmWaitSeconds,
            Vps4ApiClient apiClient, Vps4ApiClient adminClient, String shopperId,
            ExecutorService threadPool){

        this.maxTotalVmCount = maxTotalVmCount;
        this.maxPerImageVmCount = maxImageVmCount;
        this.maxVmWaitSeconds = maxVmWaitSeconds;

        this.vmLeases = new Semaphore(maxTotalVmCount);
        this.apiClient = apiClient;
        this.adminClient = adminClient;
        this.shopperId = shopperId;
        this.threadPool = threadPool;
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
                        // we _can_ spin one up if a credit is available
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
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            logger.debug("acquired {}", vm);
            return vm;
        }

        public void createVm() {
            threadPool.submit(() -> {
                UUID orionGuid = getVmCredit();
                if (orionGuid == null) {
                    vmLeases.release();
                    logger.error("No credit available to create vm {}", imageName);
                }
                else {
                // Create the VM and add to the pool
                    logger.debug("creating vm for {}, using credit guid {}", imageName, orionGuid);
                    UUID vmId = provisionVm(orionGuid);

                    offer( new VirtualMachine(
                            VirtualMachinePool.this,
                            VirtualMachinePool.this.apiClient,
                            this.imageName,
                            username,
                            password,
                            vmId));
            }});
        }

        public UUID getVmCredit() {
            // Check if a vm credit is available for pool image via hfs ecomm api
            // Regex example: hfs-centos-7-cpanel-11, where <os> is centos, and <panel> is cpanel
            String pattern = "(?:hfs-)?(?<os>\\w*)-\\w*(?:-(?<panel>\\w*)-\\w*)?";
            Matcher m = Pattern.compile(pattern).matcher(imageName);
            m.matches();
            String os = m.group("os").equals("windows") ? "windows" : "Linux";
            String panel = m.group("panel") != null ? m.group("panel") :  "MYH";
            return apiClient.getVmCredit(shopperId,  os,  panel);
        }

//        public UUID createVmCredit(){
//            // credits should no longer be created via vps4 api, but instead should now be
//            // created in Orion -> HFS Orion Listener -> Cassandra and retrieved thru ecomm api
//            // However, hfs ecomm api does allow creating credits if necessary (eg. staging)
//
//            String controlPanel = "MYH";
//            if(imageName.toUpperCase().contains("CPANEL")){
//                controlPanel = "cPanel";
//            }
//            else if(imageName.toUpperCase().contains("PLESK")){
//                controlPanel = "Plesk";
//            }
//
//            String os = "Linux";
//            if(imageName.toUpperCase().contains("WIN")){
//                os = "windows";
//            }
//            return adminClient.createVmCredit(shopperId, os, controlPanel, 0, 10);
//        }

        static final String username = "vpstester";
        static final String password = "thisvps4TEST!";

        public UUID provisionVm(UUID orionGuid){
            JSONObject provisionResult = apiClient.provisionVm("VPS4 Phase 3 Test VM",
                    orionGuid, imageName, 1, username, password);

            UUID vmId = UUID.fromString(provisionResult.get("virtualMachineId").toString());

            String actionId = provisionResult.get("id").toString();
            apiClient.pollForVmActionComplete(vmId, actionId, maxVmWaitSeconds);
            return vmId;
        }

    }

}
