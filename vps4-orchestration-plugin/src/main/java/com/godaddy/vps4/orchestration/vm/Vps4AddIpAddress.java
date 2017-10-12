package com.godaddy.vps4.orchestration.vm;


import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuota;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp.BindIpRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.vm.VmService;

@CommandMetadata(
        name="Vps4AddIpAddress",
        requestType=Vps4AddIpAddress.Request.class,
        responseType=Void.class
    )
public class Vps4AddIpAddress extends ActionCommand<Vps4AddIpAddress.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4AddIpAddress.class);

    final ActionService actionService;
    final VmService vmService;
    final VirtualMachineService virtualMachineService;
    final NetworkService networkService;

    @Inject
    public Vps4AddIpAddress(ActionService actionService, VmService vmService,
            VirtualMachineService virtualMachineService, NetworkService networkService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.networkService = networkService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4AddIpAddress.Request request) throws Exception {
        logger.info("Add an IP to vm with hfsVmId {}", request.hfsVmId);
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(request.hfsVmId);
        IpAddress ip = allocateIp(context, request);
        addIpToDatabase(context, ip, virtualMachine.vmId);
        disableMailRelays(context, ip);
        bindIp(context, ip, virtualMachine.hfsVmId);
        logger.info("Completed adding IP {} to vm with Id {} and hfsVmId {}", ip.address, virtualMachine.vmId, request.hfsVmId);

        return null;
    }

    private IpAddress allocateIp(CommandContext context,
            Vps4AddIpAddress.Request request) {
        AllocateIp.Request allocateIpRequest = new AllocateIp.Request();
        allocateIpRequest.sgid = request.sgid;
        allocateIpRequest.zone = request.zone;

        logger.info("Allocating IP for sgid {} in zone {}" , allocateIpRequest.sgid, allocateIpRequest.zone);
        IpAddress ip = context.execute(AllocateIp.class, allocateIpRequest);
        return ip;
    }

    private void addIpToDatabase(CommandContext context, IpAddress ip, UUID vmId){
        logger.info("Adding IP {} to the db for vmId {}", ip.address, vmId);
        context.execute("Create-" + ip.addressId, ctx -> {
             networkService.createIpAddress(ip.addressId, vmId, ip.address, IpAddressType.SECONDARY);
             return null;
        }, Void.class);
    }

    private void disableMailRelays(CommandContext context, IpAddress ip) {
        SetMailRelayQuota.Request hfsRequest = new SetMailRelayQuota.Request();
        hfsRequest.ipAddress = ip.address;
        hfsRequest.mailRelayQuota = 0;
        context.execute(SetMailRelayQuota.class, hfsRequest);
    }

    private void bindIp(CommandContext context, IpAddress ip, long hfsVmId) {
        BindIpRequest bindRequest = new BindIpRequest();
        bindRequest.addressId = ip.addressId;
        bindRequest.vmId = hfsVmId;
        logger.info("Binding IP {} for vmId {}", bindRequest.addressId, bindRequest.vmId);
        context.execute(BindIp.class, bindRequest);
    }

    public static class Request extends VmActionRequest{
      public String sgid;
      public String zone;

      @Override
      public String toString(){
          return ("HfsVmId: " + hfsVmId + ", sgid:"+sgid+", zone:"+zone);
      }

  }

}