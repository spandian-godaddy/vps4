package com.godaddy.vps4.orchestration.vm;

import java.util.Arrays;

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
import com.godaddy.vps4.orchestration.hfs.smtp.CreateMailRelay;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.CreateVmStep;
import com.godaddy.vps4.vm.HostnameGenerator;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.ProvisionVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.mailrelay.MailRelayTarget;
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
        ProvisionVmInfo vmInfo = request.vmInfo;

        state = new ActionState();

        setStep(CreateVmStep.StartingServerSetup);

        logger.info("begin provision vm for request: {}", request);

        // allocate IP address
        setStep(CreateVmStep.RequestingIPAddress);
        AllocateIp.Request allocateIpRequest = createAllocateIpRequest(request, vmInfo);
        gdg.hfs.vhfs.network.IpAddress ip = context.execute(AllocateIp.class, allocateIpRequest);

        // create mail relay
        setStep(CreateVmStep.RequestingMailRelay);
        CreateMailRelay.Request createMailRelayRequest = new CreateMailRelay.Request(ip.address);
        MailRelayTarget mailRelay = context.execute(CreateMailRelay.class, createMailRelayRequest);

        CreateVMWithFlavorRequest hfsRequest = request.hfsRequest;
        
        // Generate a new hostname from the allocated ip
        setStep(CreateVmStep.GeneratingHostname);
        hfsRequest.hostname = HostnameGenerator.getHostname(ip.address);
        virtualMachineService.setHostname(vmInfo.vmId, hfsRequest.hostname);

        // Create the VM
        setStep(CreateVmStep.RequestingServer);
        VmAction vmAction = context.execute(CreateVm.class, hfsRequest);

        // Get the hfs vm
        Vm hfsVm = context.execute("GetVmAfterCreate", ctx -> (Vm)vmService.getVm(vmAction.vmId));
        
        // TODO update action with VM

        // VPS4 VM bookeeping (Add hfs vmid to the virtual_machine db row)
        context.execute("Vps4ProvisionVm", ctx -> {
            virtualMachineService.addHfsVmIdToVirtualMachine(vmInfo.vmId, hfsVm.vmId);
            return null;
        });
        
        // associate the Vm with the user that created it
        context.execute("CreateVps4User", ctx -> {
            vmUserService.createUser(hfsRequest.username, vmInfo.vmId);
            return null;
        });
        
        // set the root password to the same as the user password (LINUX ONLY)
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmInfo.vmId);
        if(vm.image.operatingSystem == Image.OperatingSystem.LINUX) {
            SetPassword.Request setRootPasswordRequest = createSetRootPasswordRequest(request, hfsVm);
            context.execute(SetPassword.class, setRootPasswordRequest);
        }

        // bind IP to the VM
        setStep(CreateVmStep.ConfiguringNetwork);
        BindIpRequest bindRequest = createBindIpRequest(ip, hfsVm);
        context.execute(BindIp.class, bindRequest);

        // Add the ip to the database
        context.execute("Create-"+ip.addressId, ctx -> {networkService.createIpAddress(ip.addressId, vmInfo.vmId, ip.address, IpAddressType.PRIMARY);
                                                        return null;});

        if (vmInfo.image.controlPanel == ControlPanel.CPANEL) {
            // VM with cPanel
            setStep(CreateVmStep.ConfiguringCPanel);

            // configure cpanel on the vm
            ConfigureCpanelRequest cpanelRequest = createConfigureCpanelRequest(hfsVm);
            context.execute(ConfigureCpanel.class, cpanelRequest);
        }

        // enable/disable admin access for user
        ToggleAdmin.Request toggleAdminRequest = createToggleAdminRequest(request, vmInfo, hfsVm);
        context.execute(ToggleAdmin.class, toggleAdminRequest);

        setStep(CreateVmStep.SetupComplete);
        logger.info("provision vm finished with status {} for action: {}", vmAction);
        return null;
    }
    
    private SetPassword.Request createSetRootPasswordRequest(Request request, Vm hfsVm) {
        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = hfsVm.vmId;
        String[] usernames = {"root"};
        setPasswordRequest.usernames = Arrays.asList(usernames);
        setPasswordRequest.password = request.hfsRequest.password;
        return setPasswordRequest;
    }

    private ToggleAdmin.Request createToggleAdminRequest(Request request, ProvisionVmInfo vmInfo, Vm hfsVm) {
        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.enabled = vmInfo.managedLevel < 1;
        toggleAdminRequest.vmId = hfsVm.vmId;
        toggleAdminRequest.username = request.hfsRequest.username;
        return toggleAdminRequest;
    }

    private ConfigureCpanelRequest createConfigureCpanelRequest(Vm hfsVm) {
        ConfigureCpanelRequest cpanelRequest = new ConfigureCpanelRequest();
        cpanelRequest.vmId = hfsVm.vmId;
        cpanelRequest.publicIp = hfsVm.address.ip_address;
        return cpanelRequest;
    }

    private BindIpRequest createBindIpRequest(gdg.hfs.vhfs.network.IpAddress ip, Vm hfsVm) {
        BindIpRequest bindRequest = new BindIpRequest();
        bindRequest.addressId = ip.addressId;
        bindRequest.vmId = hfsVm.vmId;
        return bindRequest;
    }

    private AllocateIp.Request createAllocateIpRequest(Request request, ProvisionVmInfo vmInfo) {
        AllocateIp.Request allocation = new AllocateIp.Request();
        allocation.sgid = vmInfo.sgid;
        allocation.zone = request.hfsRequest.zone;
        return allocation;
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
