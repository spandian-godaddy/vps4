package com.godaddy.vps4.orchestration.vm;

import java.util.Arrays;
import java.util.UUID;

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
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
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
import gdg.hfs.vhfs.mailrelay.MailRelay;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import gdg.hfs.vhfs.mailrelay.MailRelayUpdate;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.nodeping.CheckType;
import gdg.hfs.vhfs.nodeping.CreateCheckRequest;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

@CommandMetadata(
    name="ProvisionVm",
    requestType=Vps4ProvisionVm.Request.class,
    responseType=Vps4ProvisionVm.Response.class
)
public class Vps4ProvisionVm extends ActionCommand<Vps4ProvisionVm.Request, Vps4ProvisionVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ProvisionVm.class);

    final VmService vmService;

    final VirtualMachineService virtualMachineService;

    final VmUserService vmUserService;

    final NetworkService networkService;

    final MailRelayService mailRelayService;

    final NodePingService monitoringService;

    Request request;

    ActionState state;

    String hostname;

    CommandContext context;

    @Inject
    public Vps4ProvisionVm(ActionService actionService,
                    VmService vmService,
                    VirtualMachineService virtualMachineService,
                    VmUserService vmUserService,
            NetworkService networkService,
            MailRelayService mailRelayService,
            NodePingService monitoringService) {
        super(actionService);
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.vmUserService = vmUserService;
        this.networkService = networkService;
        this.mailRelayService = mailRelayService;
        this.monitoringService = monitoringService;
    }

    @Override
    public Response executeWithAction(CommandContext context, Request request) throws Exception {

        this.request = request;
        this.context = context;
        this.state = new ActionState();

        setStep(CreateVmStep.StartingServerSetup);

        logger.info("begin provision vm for request: {}", request);

        IpAddress ip = allocateIp();

        createMailRelay(ip);

        Vm hfsVm = createVm(ip);

        setupUsers(hfsVm);

        bindIp(hfsVm, ip);

        configureControlPanel(hfsVm);

        setHostname(hfsVm);

        configureAdminUser(hfsVm, request.vmInfo.vmId);

        configureMailRelay(hfsVm);

        configureNodePing(ip);

        setStep(CreateVmStep.SetupComplete);
        logger.info("provision vm finished: {}", hfsVm);

        return null;
    }

    private void setHostname(Vm hfsVm){
        setStep(CreateVmStep.SetHostname);

        SetHostname.Request hfsRequest = new SetHostname.Request(hfsVm.vmId, hostname,
                                                request.vmInfo.image.controlPanel.toString());

        context.execute(SetHostname.class, hfsRequest);
    }

    private void configureControlPanel(Vm hfsVm) {
        if (request.vmInfo.image.controlPanel == ControlPanel.CPANEL) {
            // VM with cPanel
            setStep(CreateVmStep.ConfiguringCPanel);

            // configure cpanel on the vm
            ConfigureCpanelRequest cpanelRequest = createConfigureCpanelRequest(hfsVm);
            context.execute(ConfigureCpanel.class, cpanelRequest);

        } else if (request.vmInfo.image.controlPanel == ControlPanel.PLESK) {
            // VM with Plesk image
            setStep(CreateVmStep.ConfiguringPlesk);

            // configure Plesk on the vm
            ConfigurePleskRequest pleskRequest = createConfigurePleskRequest(hfsVm, request);
            context.execute(ConfigurePlesk.class, pleskRequest);
        }
    }

    private void bindIp(Vm hfsVm, IpAddress ip) {
        // bind IP to the VM
        setStep(CreateVmStep.ConfiguringNetwork);

        BindIpRequest bindRequest = new BindIpRequest();
        bindRequest.addressId = ip.addressId;
        bindRequest.vmId = hfsVm.vmId;
        context.execute(BindIp.class, bindRequest);

        // Add the ip to the database
        context.execute("Create-" + ip.addressId, ctx -> {
            networkService.createIpAddress(ip.addressId, request.vmInfo.vmId, ip.address, IpAddressType.PRIMARY);
            return null;
        });
    }

    private void configureMailRelay(Vm hfsVm) {
        setStep(CreateVmStep.ConfigureMailRelay);

        String controlPanel = request.vmInfo.image.controlPanel.equals(ControlPanel.MYH) ? null
                : request.vmInfo.image.controlPanel.name().toLowerCase();

        ConfigureMailRelayRequest configureMailRelayRequest = new ConfigureMailRelayRequest(hfsVm.vmId,
                controlPanel);
        context.execute(ConfigureMailRelay.class, configureMailRelayRequest);

    }

    private void setupUsers(Vm hfsVm) {
        // associate the Vm with the user that created it
        context.execute("CreateVps4User", ctx -> {
            vmUserService.createUser(request.hfsRequest.username, request.vmInfo.vmId);
            return null;
        });

        // set the root password to the same as the user password (LINUX ONLY)
        VirtualMachine vm = virtualMachineService.getVirtualMachine(request.vmInfo.vmId);
        if(vm.image.operatingSystem == Image.OperatingSystem.LINUX) {
            SetPassword.Request setRootPasswordRequest = createSetRootPasswordRequest(request, hfsVm);
            context.execute(SetPassword.class, setRootPasswordRequest);
        }
    }

    private Vm createVm(IpAddress ip) {
        CreateVMWithFlavorRequest hfsRequest = request.hfsRequest;

        // Generate a new hostname from the allocated ip
        setStep(CreateVmStep.GeneratingHostname);
        hostname = HostnameGenerator.getHostname(ip.address);
        hfsRequest.hostname = hostname;
        virtualMachineService.setHostname(request.vmInfo.vmId, hfsRequest.hostname);

        // Create the VM
        setStep(CreateVmStep.RequestingServer);
        VmAction vmAction = context.execute(CreateVm.class, hfsRequest);

        // Get the hfs vm
        Vm hfsVm = context.execute("GetVmAfterCreate", ctx -> vmService.getVm(vmAction.vmId));

        // VPS4 VM bookeeping (Add hfs vmid to the virtual_machine db row)
        context.execute("Vps4ProvisionVm", ctx -> {
            virtualMachineService.addHfsVmIdToVirtualMachine(request.vmInfo.vmId, hfsVm.vmId);
            return null;
        });

        return hfsVm;
    }

    private void createMailRelay(IpAddress ip) {

        setStep(CreateVmStep.RequestingMailRelay);

        MailRelayUpdate mailRelayUpdate = new MailRelayUpdate();
        mailRelayUpdate.quota = request.vmInfo.mailRelayQuota;
        MailRelay relay = mailRelayService.setRelayQuota(ip.address, mailRelayUpdate);
        if (relay == null || relay.quota != mailRelayUpdate.quota) {
            throw new RuntimeException(String
                    .format("Failed to create mail relay for ip %s. Provision will not continue, please fix the mailRelay", ip.address));
        }
    }

    private SetPassword.Request createSetRootPasswordRequest(Request request, Vm hfsVm) {
        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = hfsVm.vmId;
        String[] usernames = {"root"};
        setPasswordRequest.usernames = Arrays.asList(usernames);
        setPasswordRequest.password = request.hfsRequest.password;
        return setPasswordRequest;
    }

    private void configureAdminUser(Vm hfsVm, UUID vmId) {
        boolean adminEnabled = request.vmInfo.image.controlPanel == ControlPanel.MYH;
        String username = request.hfsRequest.username;
        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.enabled = adminEnabled;
        toggleAdminRequest.vmId = hfsVm.vmId;
        toggleAdminRequest.username = username;

        context.execute(ToggleAdmin.class, toggleAdminRequest);

        vmUserService.updateUserAdminAccess(username, vmId, adminEnabled);
    }

    private ConfigureCpanelRequest createConfigureCpanelRequest(Vm hfsVm) {
        ConfigureCpanelRequest cpanelRequest = new ConfigureCpanelRequest();
        cpanelRequest.vmId = hfsVm.vmId;
        cpanelRequest.publicIp = hfsVm.address.ip_address;
        return cpanelRequest;
    }

    private ConfigurePleskRequest createConfigurePleskRequest(Vm hfsVm, Request request) {
        ConfigurePleskRequest pleskRequest = new ConfigurePleskRequest(hfsVm.vmId, request.hfsRequest.username, request.hfsRequest.password);
        return pleskRequest;
    }

    private IpAddress allocateIp() {

        setStep(CreateVmStep.RequestingIPAddress);

        AllocateIp.Request allocateIpRequest = new AllocateIp.Request();
        allocateIpRequest.sgid = request.vmInfo.sgid;
        allocateIpRequest.zone = request.hfsRequest.zone;

        return context.execute(AllocateIp.class, allocateIpRequest);
    }

    private void configureNodePing(IpAddress ipAddress) {
        if (request.vmInfo.pingCheckAccountId > 0) {
            setStep(CreateVmStep.ConfigureNodeping);
            CreateCheckRequest checkRequest = new CreateCheckRequest();
            checkRequest.target = ipAddress.address;
            checkRequest.label = ipAddress.address;
            checkRequest.interval = 1;
            checkRequest.type = CheckType.PING;

            NodePingCheck check = monitoringService.createCheck(request.vmInfo.pingCheckAccountId, checkRequest);
            logger.debug("CheckId: {}", check.checkId);

            // Add the checkId to the IpAddress
            context.execute("AddCheckIdToIp-" + ipAddress.address, ctx -> {
                networkService.updateIpWithCheckId(ipAddress.addressId, check.checkId);
                return null;
            });
        }
    }

    protected void setStep(CreateVmStep step) {
        state.step = step;
        try {
            actionService.updateActionState(request.getActionId(), mapper.writeValueAsString(state));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
