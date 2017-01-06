package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel.ConfigureCpanelRequest;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp.BindIpRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.CreateVmStep;
import com.godaddy.vps4.vm.HostnameGenerator;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.ProvisionVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

@CommandMetadata(
    name="ProvisionVm",
    requestType=ProvisionVm.Request.class,
    responseType=ProvisionVm.Response.class
)
public class ProvisionVm extends ActionCommand<ProvisionVm.Request, ProvisionVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(ProvisionVm.class);

    final VmService vmService;

    final VirtualMachineService virtualMachineService;

    final VmUserService vmUserService;

    final NetworkService networkService;

    Request request;

    ActionState state;

    @Inject
    public ProvisionVm(ActionService actionService,
                    VmService vmService,
                    VirtualMachineService virtualMachineService,
                    VmUserService vmUserService,
                    NetworkService networkService) {
        super(actionService);
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.vmUserService = vmUserService;
        this.networkService = networkService;
    }

    @Override
    public Response executeWithAction(CommandContext context, Request request) throws Exception {

        this.request = request;

        state = new ActionState();

        setStep(CreateVmStep.StartingServerSetup);

        logger.info("begin provision vm for request: {}", request);

        // allocate IP address
        setStep(CreateVmStep.RequestingIPAddress);

        String sgid = request.vmInfo.sgid;

        AllocateIp.Request allocation = new AllocateIp.Request();
        allocation.sgid = sgid;
        allocation.zone = request.hfsRequest.zone;

        gdg.hfs.vhfs.network.IpAddress ip = context.execute(AllocateIp.class, allocation);

        // provision the VM
        ProvisionVmInfo vmInfo = request.vmInfo;

        CreateVMWithFlavorRequest hfsRequest = request.hfsRequest;

        setStep(CreateVmStep.GeneratingHostname);

        hfsRequest.hostname = HostnameGenerator.getHostname(ip.address);

        setStep(CreateVmStep.RequestingServer);

        VmAction vmAction = context.execute(CreateVm.class, hfsRequest);

        // TODO update action with VM
        Vm hfsVm = context.execute("GetVmAfterCreate", ctx -> (Vm)vmService.getVm(vmAction.vmId));
        

        // VPS4 VM bookeeping
        context.execute("Vps4ProvisionVm", ctx -> {
            virtualMachineService.provisionVirtualMachine(hfsVm.vmId, vmInfo.orionGuid, vmInfo.name, vmInfo.projectId,
                    vmInfo.specId, vmInfo.managedLevel, vmInfo.image.imageId);
            return null;
        });

        VirtualMachine vm = virtualMachineService.getVirtualMachine(hfsVm.vmId);
        
        // associate the Vm with the user that created it
        context.execute("CreateVps4User", ctx -> {
            vmUserService.createUser(hfsRequest.username, vm.id);
            return null;
        });

        // bind IP
        setStep(CreateVmStep.ConfiguringNetwork);
        //BindIpAction bindIpAction = new BindIpAction(ip.addressId, ip.address, hfsAction.vmId, IpAddressType.PRIMARY);
        BindIpRequest bindRequest = new BindIpRequest();
        bindRequest.addressId = ip.addressId;
        bindRequest.vmId = hfsVm.vmId;
        context.execute(BindIp.class, bindRequest);

        context.execute("Create-"+ip.addressId, ctx -> {networkService.createIpAddress(ip.addressId, vm.id, ip.address, IpAddressType.PRIMARY);
                                                        return null;});


        if (vmInfo.image.controlPanel == ControlPanel.CPANEL) {

            setStep(CreateVmStep.ConfiguringCPanel);

            ConfigureCpanelRequest cpanelRequest = new ConfigureCpanelRequest();
            cpanelRequest.vmId = hfsVm.vmId;
            cpanelRequest.publicIp = hfsVm.address.ip_address;

            context.execute(ConfigureCpanel.class, cpanelRequest);
        }

        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.enabled = vmInfo.managedLevel < 1;
        toggleAdminRequest.vmId = hfsVm.vmId;
        toggleAdminRequest.username = request.hfsRequest.username;
        context.execute(ToggleAdmin.class, toggleAdminRequest);

        setStep(CreateVmStep.SetupComplete);

        logger.info("provision vm finished with status {} for action: {}", vmAction);

        return null;
    }

    protected void setStep(CreateVmStep step) throws JsonProcessingException {
        state.step = step;
        actionService.updateActionState(request.getActionId(), mapper.writeValueAsString(state));
    }

    public static class Request implements ActionRequest {
        public CreateVMWithFlavorRequest hfsRequest;
        public ProvisionVmInfo vmInfo;
        public long actionId;

        @Override
        public long getActionId() {
            return actionId;
        }
    }

    public static class Response {
        public long vmId;
    }

    public static class ActionState {
        public CreateVmStep step;
    }

}
