package com.godaddy.vps4.orchestration.vm.provision;

import static com.godaddy.vps4.vm.CreateVmStep.ConfigureMailRelay;
import static com.godaddy.vps4.vm.CreateVmStep.ConfigureNodeping;
import static com.godaddy.vps4.vm.CreateVmStep.ConfiguringCPanel;
import static com.godaddy.vps4.vm.CreateVmStep.ConfiguringNetwork;
import static com.godaddy.vps4.vm.CreateVmStep.ConfiguringPlesk;
import static com.godaddy.vps4.vm.CreateVmStep.GeneratingHostname;
import static com.godaddy.vps4.vm.CreateVmStep.RequestingIPAddress;
import static com.godaddy.vps4.vm.CreateVmStep.RequestingMailRelay;
import static com.godaddy.vps4.vm.CreateVmStep.RequestingServer;
import static com.godaddy.vps4.vm.CreateVmStep.SetHostname;
import static com.godaddy.vps4.vm.CreateVmStep.SetupAutomaticBackupSchedule;
import static com.godaddy.vps4.vm.CreateVmStep.SetupComplete;
import static com.godaddy.vps4.vm.CreateVmStep.StartingServerSetup;

import java.util.UUID;

import javax.inject.Inject;
import com.godaddy.hfs.config.Config;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
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
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.orchestration.scheduler.SetupAutomaticBackupSchedule;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
import com.godaddy.vps4.util.MonitoringMeta;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.CreateVmStep;
import com.godaddy.vps4.vm.HostnameGenerator;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.nodeping.CreateCheckRequest;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CommandMetadata(
        name = "ProvisionVm",
        requestType = ProvisionRequest.class,
        responseType = Vps4ProvisionVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ProvisionVm extends ActionCommand<ProvisionRequest, Vps4ProvisionVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ProvisionVm.class);
    private final VmService vmService;
    private final VirtualMachineService virtualMachineService;
    private final VmUserService vmUserService;
    private final NetworkService networkService;
    private final NodePingService monitoringService;
    private final MonitoringMeta monitoringMeta;
    private final Vps4MessagingService messagingService;
    private final CreditService creditService;
    private final Config config;

    private ProvisionRequest request;
    private ActionState state;
    private String hostname;
    private CommandContext context;

    @Inject
    public Vps4ProvisionVm(
            ActionService actionService,
            VmService vmService,
            VirtualMachineService virtualMachineService,
            VmUserService vmUserService,
            NetworkService networkService,
            NodePingService monitoringService,
            MonitoringMeta monitoringMeta,
            Vps4MessagingService messagingService,
            CreditService creditService,
            Config config) {
        super(actionService);
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.vmUserService = vmUserService;
        this.networkService = networkService;
        this.monitoringService = monitoringService;
        this.monitoringMeta = monitoringMeta;
        this.messagingService = messagingService;
        this.creditService = creditService;
        this.config = config;
    }

    @Override
    public Response executeWithAction(CommandContext context, ProvisionRequest request) {

        this.request = request;
        this.context = context;
        this.state = new ActionState();

        setStep(StartingServerSetup);

        logger.info("begin provision vm for request: {}", request);

        IpAddress ip = allocateIp();

        createMailRelay(ip);

        generateHostname(ip);

        long hfsVmId = createVm();

        bindIp(hfsVmId, ip);

        setupUsers(hfsVmId);

        configureControlPanel(hfsVmId);

        setHostname(hfsVmId);

        configureAdminUser(hfsVmId, request.vmInfo.vmId);

        configureMailRelay(hfsVmId);

        configureMonitoring(ip);

        setEcommCommonName(request.orionGuid, request.serverName);

        sendSetupEmail(request, ip.address);

        // TODO: keeps this commented until we have the nginx configured to setup client cert based auth for
        // vps4 inter microservice communication.
        setupAutomaticBackupSchedule(request.vmInfo.vmId, request.shopperId);

        setStep(SetupComplete);
        logger.info("provision vm finished: {}", request.vmInfo.vmId);
        return null;
    }

    private void generateHostname(IpAddress ip) {
        setStep(GeneratingHostname);
        hostname = HostnameGenerator.getHostname(ip.address);
    }

    private void setupAutomaticBackupSchedule(UUID vps4VmId, String shopperId) {
        setStep(SetupAutomaticBackupSchedule);
        SetupAutomaticBackupSchedule.Request req = new SetupAutomaticBackupSchedule.Request();
        req.vmId = vps4VmId;
        req.backupName = config.get("vps4.autobackup.backupName");
        req.shopperId = shopperId;
        try {
            UUID backupJobId = context.execute(SetupAutomaticBackupSchedule.class, req);
            context.execute("AddBackupJobIdToVM", ctx -> {
                virtualMachineService.setBackupJobId(vps4VmId, backupJobId);
                return null;
            }, Void.class);

        } catch (RuntimeException e) {
            // squelch this for now. dont fail a vm provisioning just because we couldn't create an auto backup schedule
            // TODO: should this behaviour be changed?
            logger.error("Automatic backup job creation failed {}", e);
        }
    }

    private void setHostname(long hfsVmId) {
        setStep(SetHostname);

        SetHostname.Request hfsRequest = new SetHostname.Request(hfsVmId, hostname,
                request.vmInfo.image.getImageControlPanel());

        context.execute(SetHostname.class, hfsRequest);
    }

    private void configureControlPanel(long hfsVmId) {
        if (request.vmInfo.image.hasCpanel()) {
            // VM with cPanel
            setStep(ConfiguringCPanel);

            // configure cpanel on the vm
            ConfigureCpanelRequest cpanelRequest = createConfigureCpanelRequest(hfsVmId);
            context.execute(ConfigureCpanel.class, cpanelRequest);

        } else if (request.vmInfo.image.hasPlesk()) {
            // VM with Plesk image
            setStep(ConfiguringPlesk);

            // configure Plesk on the vm
            ConfigurePleskRequest pleskRequest = createConfigurePleskRequest(hfsVmId);
            context.execute(ConfigurePlesk.class, pleskRequest);
        }
    }

    private void bindIp(long hfsVmId, IpAddress ip) {
        // bind IP to the VM
        setStep(ConfiguringNetwork);

        BindIpRequest bindRequest = new BindIpRequest();
        bindRequest.addressId = ip.addressId;
        bindRequest.vmId = hfsVmId;
        context.execute(BindIp.class, bindRequest);
    }

    private void configureMailRelay(long hfsVmId) {
        setStep(ConfigureMailRelay);

        ConfigureMailRelayRequest configureMailRelayRequest = new ConfigureMailRelayRequest(hfsVmId, request.vmInfo.image.controlPanel);
        context.execute(ConfigureMailRelay.class, configureMailRelayRequest);

    }

    private void setupUsers(long hfsVmId) {
        // associate the Vm with the user that created it
        context.execute("CreateVps4User", ctx -> {
            vmUserService.createUser(request.username, request.vmInfo.vmId);
            return null;
        }, Void.class);

        // set the root password to the same as the user password (LINUX ONLY)
        VirtualMachine vm = virtualMachineService.getVirtualMachine(request.vmInfo.vmId);
        if (vm.image.operatingSystem == Image.OperatingSystem.LINUX) {
            SetPassword.Request setRootPasswordRequest
                = ProvisionHelper.createSetRootPasswordRequest(hfsVmId, request.encryptedPassword, vm.image.getImageControlPanel());
            context.execute(SetPassword.class, setRootPasswordRequest);
        }
    }

    private long createVm() {
        setStep(RequestingServer);
        CreateVm.Request createVmRequest = ProvisionHelper.getCreateVmRequest(request, hostname);
        virtualMachineService.setHostname(request.vmInfo.vmId, createVmRequest.hostname);
        VmAction vmAction = context.execute(CreateVm.class, createVmRequest);
        // note: we want to update the HFS vm id in the vps4 database in the event
        // that the provisioning failed on the HFS side.
        // This makes tracking down the VM easier for us with an HFS vm id.
        addHfsVmIdToVmInVps4Db(vmAction);
        // wait for the vm action to complete here
        context.execute(WaitForVmAction.class, vmAction);
        return vmAction.vmId;
    }

    private void addHfsVmIdToVmInVps4Db(VmAction vmAction) {
        context.execute("Vps4ProvisionVm", ctx -> {
            virtualMachineService.addHfsVmIdToVirtualMachine(request.vmInfo.vmId, vmAction.vmId);
            return null;
        }, Void.class);
    }

    private void createMailRelay(IpAddress ip) {

        setStep(RequestingMailRelay);

        SetMailRelayQuota.Request hfsRequest = new SetMailRelayQuota.Request();
        hfsRequest.ipAddress = ip.address;
        hfsRequest.mailRelayQuota = request.vmInfo.mailRelayQuota;
        context.execute(SetMailRelayQuota.class, hfsRequest);
    }

    private void configureAdminUser(long hfsVmId, UUID vmId) {
        ToggleAdmin.Request toggleAdminRequest = ProvisionHelper.getToggleAdminRequest(request, hfsVmId);
        context.execute(ToggleAdmin.class, toggleAdminRequest);
        vmUserService.updateUserAdminAccess(toggleAdminRequest.username, vmId, toggleAdminRequest.enabled);
    }

    private ConfigureCpanelRequest createConfigureCpanelRequest(long vmId) {
        ConfigureCpanelRequest cpanelRequest = new ConfigureCpanelRequest();
        cpanelRequest.vmId = vmId;
        return cpanelRequest;
    }

    private ConfigurePleskRequest createConfigurePleskRequest(long hfsVmId) {
        return new ConfigurePleskRequest(hfsVmId, request.username, request.encryptedPassword);
    }

    private IpAddress allocateIp() {

        setStep(RequestingIPAddress);

        AllocateIp.Request allocateIpRequest = new AllocateIp.Request();
        allocateIpRequest.sgid = request.vmInfo.sgid;
        allocateIpRequest.zone = request.zone;

        IpAddress ip = context.execute(AllocateIp.class, allocateIpRequest);

        // Add the ip to the database
        context.execute("Create-" + ip.addressId, ctx -> {
            networkService.createIpAddress(ip.addressId, request.vmInfo.vmId, ip.address, IpAddressType.PRIMARY);
            return null;
        }, Void.class);

        return ip;
    }

    private void configureMonitoring(IpAddress ipAddress) {
        if (request.vmInfo.hasMonitoring) {
            setStep(ConfigureNodeping);
            CreateCheckRequest checkRequest = ProvisionHelper.getCreateCheckRequest(ipAddress.address, monitoringMeta);
            NodePingCheck check = monitoringService.createCheck(monitoringMeta.getAccountId(), checkRequest);
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

    private void setEcommCommonName(UUID orionGuid, String commonName) {
        context.execute("SetCommonName", ctx -> {
            creditService.setCommonName(orionGuid, commonName);
            return null;
        }, Void.class);
    }

    private void sendSetupEmail(ProvisionRequest request, String ipAddress) {
        try {
            String messageId = messagingService.sendSetupEmail(request.shopperId, request.serverName, ipAddress,
                    request.orionGuid.toString(), request.vmInfo.isFullyManaged());
            logger.info(String.format("Setup email sent for shopper %s. Message id: %s", request.shopperId, messageId));
        } catch (Exception ex) {
            logger.error(
                    String.format("Failed sending setup email for shopper %s: %s", request.shopperId, ex.getMessage()),
                    ex);
        }
    }

    private void setStep(CreateVmStep step) {
        state.step = step;
        try {
            actionService.updateActionState(request.getActionId(), mapper.writeValueAsString(state));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Response {
        public long vmId;
    }

    public static class ActionState {
        public CreateVmStep step;
    }

}
