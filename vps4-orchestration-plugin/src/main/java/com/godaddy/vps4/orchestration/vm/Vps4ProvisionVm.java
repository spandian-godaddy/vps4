package com.godaddy.vps4.orchestration.vm;

import java.util.Arrays;
import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.scheduler.SetupAutomaticBackupSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel.ConfigureCpanelRequest;
import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuota;
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
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.nodeping.CheckType;
import gdg.hfs.vhfs.nodeping.CreateCheckRequest;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;
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
    private final VmService vmService;
    private final VirtualMachineService virtualMachineService;
    private final VmUserService vmUserService;
    private final NetworkService networkService;
    private final NodePingService monitoringService;
    private final Vps4MessagingService messagingService;
    private final CreditService creditService;

    Request request;

    ActionState state;

    String hostname;

    CommandContext context;


    @Inject
    public Vps4ProvisionVm(
            ActionService actionService,
            VmService vmService,
            VirtualMachineService virtualMachineService,
            VmUserService vmUserService,
            NetworkService networkService,
            NodePingService monitoringService,
            Vps4MessagingService messagingService,
            CreditService creditService) {
        super(actionService);
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.vmUserService = vmUserService;
        this.networkService = networkService;
        this.monitoringService = monitoringService;
        this.messagingService = messagingService;
        this.creditService = creditService;
    }

    @Override
    public Response executeWithAction(CommandContext context, Request request) throws Exception {

        this.request = request;
        this.context = context;
        this.state = new ActionState();

        setStep(CreateVmStep.StartingServerSetup);

        logger.info("begin provision vm for request: {}", request);

        IpAddress ip = allocateIp();

        hostname = HostnameGenerator.getHostname(ip.address);

        createMailRelay(ip);

        Vm hfsVm = createVm(ip);

        setupUsers(hfsVm);

        bindIp(hfsVm, ip);

        configureControlPanel(hfsVm);

        setHostname(hfsVm);

        configureAdminUser(hfsVm, request.vmInfo.vmId);

        configureMailRelay(hfsVm);

        configureMonitoring(ip);

        setEcommCommonName(request.orionGuid, request.serverName);

        sendSetupEmail(request, ip.address);

        // TODO: keeps this commented until we have the nginx configured to setup client cert based auth for
        // vps4 inter microservice communication.
        if(request.scheduleBackup)
            setupAutomaticBackupSchedule(request.vmInfo.vmId, request.shopperId);

        setStep(CreateVmStep.SetupComplete);
        logger.info("provision vm finished: {}", hfsVm);

        return null;
    }

    private void setupAutomaticBackupSchedule(UUID vps4VmId, String shopperId) {
        setStep(CreateVmStep.SetupAutomaticBackupSchedule);
        SetupAutomaticBackupSchedule.Request req = new SetupAutomaticBackupSchedule.Request();
        req.vmId = vps4VmId;
        req.backupName = "autoBackup";
        req.shopperId = shopperId;
        try {
            UUID backupJobId = context.execute(SetupAutomaticBackupSchedule.class, req);
            context.execute("AddBackupJobIdToVM", ctx -> {
                virtualMachineService.setBackupJobId(vps4VmId, backupJobId);
                return null;
            }, Void.class);

        }
        catch (RuntimeException e) {
            // squelch this for now. dont fail a vm provisioning just because we couldn't create an auto backup schedule
            // TODO: should this behaviour be changed?
            logger.error("Automatic backup job creation failed");
        }
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
            ConfigureCpanelRequest cpanelRequest = createConfigureCpanelRequest(hfsVm.vmId);
            context.execute(ConfigureCpanel.class, cpanelRequest);

        } else if (request.vmInfo.image.controlPanel == ControlPanel.PLESK) {
            // VM with Plesk image
            setStep(CreateVmStep.ConfiguringPlesk);

            // configure Plesk on the vm
            ConfigurePleskRequest pleskRequest = createConfigurePleskRequest(hfsVm);
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
        }, Void.class);
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
            vmUserService.createUser(request.username, request.vmInfo.vmId);
            return null;
        }, Void.class);

        // set the root password to the same as the user password (LINUX ONLY)
        VirtualMachine vm = virtualMachineService.getVirtualMachine(request.vmInfo.vmId);
        if(vm.image.operatingSystem == Image.OperatingSystem.LINUX) {
            SetPassword.Request setRootPasswordRequest = createSetRootPasswordRequest(hfsVm);
            context.execute(SetPassword.class, setRootPasswordRequest);
        }
    }

    private Vm createVm(IpAddress ip) {
        // Generate a new hostname from the allocated ip
        setStep(CreateVmStep.GeneratingHostname);

        CreateVm.Request createVmRequest = new CreateVm.Request();
        createVmRequest.hostname = hostname;
        createVmRequest.image_name = request.image_name;
        createVmRequest.rawFlavor = request.rawFlavor;
        createVmRequest.sgid = request.sgid;
        createVmRequest.username = request.username;
        createVmRequest.zone = request.zone;
        createVmRequest.encryptedPassword = request.encryptedPassword;

        virtualMachineService.setHostname(request.vmInfo.vmId, createVmRequest.hostname);

        // Create the VM
        setStep(CreateVmStep.RequestingServer);
        VmAction vmAction = context.execute(CreateVm.class, createVmRequest);

        // Get the hfs vm
        Vm hfsVm = context.execute("GetVmAfterCreate", ctx -> vmService.getVm(vmAction.vmId), Vm.class);

        // VPS4 VM bookeeping (Add hfs vmid to the virtual_machine db row)
        context.execute("Vps4ProvisionVm", ctx -> {
            virtualMachineService.addHfsVmIdToVirtualMachine(request.vmInfo.vmId, hfsVm.vmId);
            return null;
        }, Void.class);

        return hfsVm;
    }

    private void createMailRelay(IpAddress ip) {

        setStep(CreateVmStep.RequestingMailRelay);

        SetMailRelayQuota.Request hfsRequest = new SetMailRelayQuota.Request();
        hfsRequest.ipAddress = ip.address;
        hfsRequest.mailRelayQuota = request.vmInfo.mailRelayQuota;
        context.execute(SetMailRelayQuota.class, hfsRequest);
    }

    private SetPassword.Request createSetRootPasswordRequest(Vm hfsVm) {
        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = hfsVm.vmId;
        String[] usernames = {"root"};
        setPasswordRequest.usernames = Arrays.asList(usernames);
        setPasswordRequest.encryptedPassword = request.encryptedPassword;
        return setPasswordRequest;
    }

    private void configureAdminUser(Vm hfsVm, UUID vmId) {
        boolean adminEnabled = request.vmInfo.image.controlPanel == ControlPanel.MYH;
        String username = request.username;
        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.enabled = adminEnabled;
        toggleAdminRequest.vmId = hfsVm.vmId;
        toggleAdminRequest.username = username;

        context.execute(ToggleAdmin.class, toggleAdminRequest);

        vmUserService.updateUserAdminAccess(username, vmId, adminEnabled);
    }

    private ConfigureCpanelRequest createConfigureCpanelRequest(long vmId) {
        ConfigureCpanelRequest cpanelRequest = new ConfigureCpanelRequest();
        cpanelRequest.vmId = vmId;
        return cpanelRequest;
    }

    private ConfigurePleskRequest createConfigurePleskRequest(Vm hfsVm) {
        ConfigurePleskRequest pleskRequest = new ConfigurePleskRequest(hfsVm.vmId, request.username, request.encryptedPassword);
        return pleskRequest;
    }

    private IpAddress allocateIp() {

        setStep(CreateVmStep.RequestingIPAddress);

        AllocateIp.Request allocateIpRequest = new AllocateIp.Request();
        allocateIpRequest.sgid = request.vmInfo.sgid;
        allocateIpRequest.zone = request.zone;

        return context.execute(AllocateIp.class, allocateIpRequest);
    }

    private void configureMonitoring(IpAddress ipAddress) {
        if (request.vmInfo.monitoringAccountId > 0) {
            setStep(CreateVmStep.ConfigureNodeping);
            CreateCheckRequest checkRequest = new CreateCheckRequest();
            checkRequest.target = ipAddress.address;
            checkRequest.label = ipAddress.address;
            checkRequest.interval = 1;
            checkRequest.type = CheckType.PING;
            // TODO: Make this hfs callback useful - notify support?
            checkRequest.webhookUrl = "http://www.godaddy.com";

            NodePingCheck check = monitoringService.createCheck(request.vmInfo.monitoringAccountId, checkRequest);
            logger.debug("CheckId: {}", check.checkId);

            addCheckIdToIp(ipAddress, check);
        }
    }

    private void addCheckIdToIp(IpAddress ipAddress, NodePingCheck check) {
        context.execute("AddCheckIdToIp-" + ipAddress.address, ctx -> {
            networkService.updateIpWithCheckId(ipAddress.addressId, check.checkId);
            return null;
        }, Void.class);
    }

    private void setEcommCommonName(UUID orionGuid, String commonName){
        context.execute("SetCommonName", ctx -> {
            creditService.setCommonName(orionGuid, commonName);
            return null;
        }, Void.class);
    }

    private void sendSetupEmail(Request request, String ipAddress) {
        try {
            String messageId = messagingService.sendSetupEmail(request.shopperId, request.serverName, ipAddress,
                    request.orionGuid.toString(), request.vmInfo.isFullyManaged());
            logger.info(String.format("Setup email sent for shopper %s. Message id: %s", request.shopperId, messageId));
        }
        catch (Exception ex) {
            logger.error(String.format("Failed sending setup email for shopper %s: %s",
                    request.shopperId, ex.getMessage()), ex);
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
        public ProvisionVmInfo vmInfo;
        public String shopperId;
        public String serverName;
        public byte[] encryptedPassword;
        public long actionId;
        public UUID orionGuid;
        public String sgid;
        public String image_name;
        public String rawFlavor;
        public String username;
        public String zone;
        public boolean scheduleBackup;

        @Override
        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }

    public static class Response {
        public long vmId;
    }

    public static class ActionState {
        public CreateVmStep step;
    }

}
