package com.godaddy.vps4.phase3.virtualmachine;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;

public class VirtualMachinePool {

    private static final Logger logger = LoggerFactory.getLogger(VirtualMachinePool.class);

    final int maxTotalVmCount;

    final int maxPerImageVmCount;

    final Semaphore vmLeases;

    final Map<String, PerImagePool> poolByImageName = new ConcurrentHashMap<>();

    final Vps4ApiClient apiClient;
    
    final String user;

    public VirtualMachinePool(int maxTotalVmCount, int maxImageVmCount, Vps4ApiClient apiClient, String user){

        this.maxTotalVmCount = maxTotalVmCount;
        this.maxPerImageVmCount = maxImageVmCount;

        this.vmLeases = new Semaphore(maxTotalVmCount);
        this.apiClient = apiClient;
        this.user = user;
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

        public PerImagePool(String imageName) {
            this.imageName = imageName;
            this.pool = new LinkedBlockingDeque<>(VirtualMachinePool.this.maxPerImageVmCount);
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
                logger.trace("pool '{}' at capacity, destroying {}", imageName, vm);
                // pool is already at its capacity, so just destroy the VM
                destroy(vm);
            }
        }

        public VirtualMachine get() {
            // if we have a VM available, return it

            // if we don't have a VM, but are under our per-image limit, create one

            // if we don't have a VM, and we can't create one, wait for a VM to
            // be returned to the pool
            VirtualMachine vm = pool.pollFirst();
            
            if (vm == null) {
                // no pooled VMs, can we spin one up?
                if (vmLeases.tryAcquire()) {
                    logger.trace("leased {}", imageName);
                    // we _can_ spin one up
                    vm = createVm();
                } else {
                    // we _can't_ spin one up, so we have to wait until one is returned
                    // to the pool
                    logger.trace("no leases available, waiting for {}", imageName);
                    try {
                        vm = pool.takeFirst();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            logger.trace("acquired {}", vm);
            return vm;
        }

        public VirtualMachine createVm() {
            // Create the VM and add to the pool
            UUID orionGuid = createVmCredit();
            UUID vmId = provisionVm(orionGuid);
            
            // FIXME 'username'/'password' must be externalized

            return new VirtualMachine(
                    VirtualMachinePool.this,
                    VirtualMachinePool.this.apiClient,
                    this.imageName,
                    username,
                    password,
                    vmId);
        }

        public UUID createVmCredit(){
            String controlPanel = "none";
            if(imageName.toUpperCase().contains("CPANEL")){
                controlPanel = "cpanel";
            }
            else if(imageName.toUpperCase().contains("PLESK")){
                controlPanel = "plesk";
            }

            String os = "linux";
            if(imageName.toUpperCase().contains("WIN")){
                os = "windows";
            }
            return apiClient.createVmCredit(user, os, controlPanel, 0, 10);
        }

        static final String username = "vpstester";
        static final String password = "thisvps4TEST!";

        public UUID provisionVm(UUID orionGuid){
            JSONObject provisionResult = apiClient.provisionVm("VPS4 Phase 3 Test VM",
                    orionGuid, imageName, 1, username, password);

            UUID vmId = UUID.fromString(provisionResult.get("virtualMachineId").toString());

            String actionId = provisionResult.get("id").toString();
            apiClient.pollForVmActionComplete(vmId, actionId, 1800); // 30 minutes :(
            return vmId;
        }

    }

}
