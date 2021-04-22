package com.godaddy.vps4.orchestration.vm.provision;

import static com.godaddy.vps4.vm.CreateVmStep.ConfigureMailRelay;
import static com.godaddy.vps4.vm.CreateVmStep.ConfigureMonitoring;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel.ConfigureCpanelRequest;
import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuota;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.orchestration.scheduler.SetupAutomaticBackupSchedule;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4RestartVm;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.CreateVmStep;
import com.godaddy.vps4.vm.HostnameGenerator;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.network.IpAddress;

@CommandMetadata(
        name = "ProvisionVm",
        requestType = ProvisionRequest.class,
        responseType = Vps4ProvisionVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ProvisionVm extends ActionCommand<ProvisionRequest, Vps4ProvisionVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ProvisionVm.class);
    private final VmService vmService;
    protected final VirtualMachineService virtualMachineService;
    private final VmUserService vmUserService;
    private final NetworkService networkService;
    private final Vps4MessagingService messagingService;
    private final CreditService creditService;
    private final Config config;
    private final HfsVmTrackingRecordService hfsVmTrackingRecordService;
    private final VmAlertService vmAlertService;

    protected ProvisionRequest request;
    private ActionState state;
    protected String hostname;
    protected CommandContext context;

    @Inject
    public Vps4ProvisionVm(
            ActionService actionService,
            VmService vmService,
            VirtualMachineService virtualMachineService,
            VmUserService vmUserService,
            NetworkService networkService,
            Vps4MessagingService messagingService,
            CreditService creditService,
            Config config,
            HfsVmTrackingRecordService hfsVmTrackingRecordService,
            VmAlertService vmAlertService) {
        super(actionService);
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.vmUserService = vmUserService;
        this.networkService = networkService;
        this.messagingService = messagingService;
        this.creditService = creditService;
        this.config = config;
        this.hfsVmTrackingRecordService = hfsVmTrackingRecordService;
        this.vmAlertService = vmAlertService;
    }

    @Override
    public Response executeWithAction(CommandContext context, ProvisionRequest request) {

        this.request = request;
        this.context = context;
        this.state = new ActionState();

        setStep(StartingServerSetup);

        logger.info("begin provision vm for request: {}", request);

        long hfsVmId = createServer();
        Vm hfsVm = vmService.getVm(hfsVmId);

        String primaryIpAddress = setupPrimaryIp(hfsVm);

        createMailRelay(primaryIpAddress);

        setupUsers(hfsVmId);

        configureControlPanel(hfsVmId);

        generateAndSetHostname(hfsVmId, primaryIpAddress, hfsVm.resourceId);

        createPTRRecord(hfsVm.resourceId);

        configureAdminUser(hfsVmId, request.vmInfo.vmId);

        configureMailRelay(hfsVmId);

        configureMonitoring(primaryIpAddress, hfsVmId);

        setEcommCommonName(request.orionGuid, request.serverName);

        sendSetupEmail(request, primaryIpAddress);

        // TODO: keeps this commented until we have the nginx configured to setup client cert based auth for
        // vps4 inter microservice communication.
        setupAutomaticBackupSchedule(request.vmInfo.vmId, request.shopperId);

        setStep(SetupComplete);
        logger.info("provision vm finished: {}", request.vmInfo.vmId);
        return null;
    }

    protected String setupPrimaryIp(Vm hfsVm) {
        IpAddress hfsIp = allocateIp();
        bindIp(hfsVm.vmId, hfsIp.addressId);
        return hfsIp.address;
    }

    /* Create Reverse DNS record.
       For Openstack/OptimizedHosting Vm, no need to set RDNS record, so skip. */
    protected void createPTRRecord(String resourceId) {
    }

    protected void setupAutomaticBackupSchedule(UUID vps4VmId, String shopperId) {
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

    protected void generateAndSetHostname(long hfsVmId, String ipAddress, String resourceId) {
        setStep(GeneratingHostname);
        hostname = HostnameGenerator.getHostname(ipAddress, request.vmInfo.image.operatingSystem);
        setHostname(hfsVmId);
    }

    protected void setHostname(long hfsVmId) {
        setStep(SetHostname);
        SetHostname.Request hfsRequest = new SetHostname.Request(hfsVmId, hostname,
                                                                 request.vmInfo.image.getImageControlPanel());
        virtualMachineService.setHostname(request.vmInfo.vmId, hostname);
        context.execute(SetHostname.class, hfsRequest);
        rebootWindowsServer();
    }

    // Windows server needs a reboot to apply hostname change
    private void rebootWindowsServer() {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(request.vmInfo.vmId);
        if (vm.image.operatingSystem == Image.OperatingSystem.WINDOWS) {
            VmActionRequest rebootRequest = new VmActionRequest();
            rebootRequest.virtualMachine = vm;
            rebootServer(rebootRequest);
        }
    }

    protected void rebootServer(VmActionRequest rebootRequest) {
        context.execute(Vps4RestartVm.class, rebootRequest);
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

    private void bindIp(long hfsVmId, long hfsAddressId) {
        // bind IP to the VM
        setStep(ConfiguringNetwork);

        BindIp.Request bindRequest = new BindIp.Request();
        bindRequest.hfsAddressId = hfsAddressId;
        bindRequest.hfsVmId = hfsVmId;
        context.execute(BindIp.class, bindRequest);
    }

    protected void configureMailRelay(long hfsVmId) {
        setStep(ConfigureMailRelay);

        ConfigureMailRelayRequest configureMailRelayRequest =
                new ConfigureMailRelayRequest(hfsVmId, request.vmInfo.image.controlPanel);
        context.execute(ConfigureMailRelay.class, configureMailRelayRequest);

    }

    private void setupUsers(long hfsVmId) {
        addUserToVps4();
        setLinuxRootPassword(hfsVmId);
    }

    private void addUserToVps4() {
        context.execute("CreateVps4User", ctx -> {
            vmUserService.createUser(request.username, request.vmInfo.vmId);
            return null;
        }, Void.class);
    }

    private void setLinuxRootPassword(long hfsVmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(request.vmInfo.vmId);
        if (vm.image.operatingSystem == Image.OperatingSystem.LINUX) {
            SetPassword.Request setRootPasswordRequest
                    = ProvisionHelper.createSetRootPasswordRequest(hfsVmId, request.encryptedPassword, vm.image.getImageControlPanel());
            context.execute(SetPassword.class, setRootPasswordRequest);
        }
    }

    private long createServer() {
        hostname = "temp.secureserver.net";
        setStep(RequestingServer);
        CreateVm.Request createVmRequest = ProvisionHelper.getCreateVmRequest(request, hostname);
        VmAction vmAction = context.execute(CreateVm.class, createVmRequest);
        // note: we want to update the HFS vm id in the vps4 database in the event
        // that the provisioning failed on the HFS side.
        // This makes tracking down the VM easier for us with an HFS vm id.
        addHfsVmIdToVmInVps4Db(vmAction);
        context.execute(WaitForAndRecordVmAction.class, vmAction);
        updateHfsVmTrackingRecord(vmAction);
        return vmAction.vmId;
    }

    private void updateHfsVmTrackingRecord(VmAction vmAction){
        context.execute("UpdateHfsVmTrackingRecord", ctx -> {
            hfsVmTrackingRecordService.setCreated(vmAction.vmId, request.actionId);
            return null;
        }, Void.class);
    }

    private void addHfsVmIdToVmInVps4Db(VmAction vmAction) {
        context.execute("AddHfsVmIdToVmInVps4Db", ctx -> {
            virtualMachineService.addHfsVmIdToVirtualMachine(request.vmInfo.vmId, vmAction.vmId);
            return null;
        }, Void.class);
    }

    protected void createMailRelay(String ipAddress) {

        setStep(RequestingMailRelay);

        SetMailRelayQuota.Request hfsRequest = new SetMailRelayQuota.Request();
        hfsRequest.ipAddress = ipAddress;
        hfsRequest.mailRelayQuota = request.vmInfo.mailRelayQuota;
        context.execute(SetMailRelayQuota.class, hfsRequest);
    }

    private void configureAdminUser(long hfsVmId, UUID vmId) {
        ToggleAdmin.Request toggleAdminRequest = ProvisionHelper.getToggleAdminRequest(request, hfsVmId);
        context.execute(ToggleAdmin.class, toggleAdminRequest);
        vmUserService.updateUserAdminAccess(toggleAdminRequest.username, vmId, toggleAdminRequest.enabled);
    }

    private ConfigureCpanelRequest createConfigureCpanelRequest(long hfsVmId) {
        return new ConfigureCpanelRequest(hfsVmId);
    }

    private ConfigurePleskRequest createConfigurePleskRequest(long hfsVmId) {
        return new ConfigurePleskRequest(hfsVmId, request.username, request.encryptedPassword);
    }

    private IpAddress allocateIp() {

        setStep(RequestingIPAddress);

        AllocateIp.Request allocateIpRequest = new AllocateIp.Request();
        allocateIpRequest.sgid = request.vmInfo.sgid;
        allocateIpRequest.zone = request.zone;

        IpAddress hfsIp = context.execute(AllocateIp.class, allocateIpRequest);

        // Add the ip to the database
        context.execute("Create-" + hfsIp.addressId, ctx -> {
            networkService.createIpAddress(hfsIp.addressId, request.vmInfo.vmId, hfsIp.address, IpAddressType.PRIMARY);
            return null;
        }, Void.class);

        return hfsIp;
    }

    /* Add IP address to VPS4 database, if it is not done already.
       For Openstack Vm, IP address has already been added to vps4 db as part of the allocateIp step. */
    protected void addIpToDb(String ipAddress) {
        context.execute("Create-" + ipAddress, ctx -> {
            networkService.createIpAddress(0, request.vmInfo.vmId, ipAddress, IpAddressType.PRIMARY);
            return null;
        }, Void.class);
    }

    private void configureMonitoring(String ipAddress, long hfsVmId) {
        // gate panopta installation using a feature flag
        if (request.vmInfo.isPanoptaEnabled) {
            installPanopta(ipAddress, hfsVmId);
            configurePanoptaAlert();
        }
    }

    private void configurePanoptaAlert() {
        if  (request.vmInfo.hasMonitoring) {
            vmAlertService.disableVmMetricAlert(request.vmInfo.vmId, VmMetric.FTP.name());
        }
    }

    private void installPanopta(String ipAddress, long hfsVmId) {
        setStep(ConfigureMonitoring);
        SetupPanopta.Request setupPanoptaRequest = new SetupPanopta.Request();
        setupPanoptaRequest.hfsVmId = hfsVmId;
        setupPanoptaRequest.orionGuid = request.orionGuid;
        setupPanoptaRequest.vmId = request.vmInfo.vmId;
        setupPanoptaRequest.shopperId = request.shopperId;
        setupPanoptaRequest.fqdn = ipAddress;
        try {
            context.execute(SetupPanopta.class, setupPanoptaRequest);
        } catch (Exception e) {
            logger.error("Exception while setting up Panopta for VM {} and shopper {}: {}",
                         request.vmInfo.vmId, request.shopperId, e);
        }
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
                    request.orionGuid.toString(), request.vmInfo.isManaged);
            logger.info(String.format("Setup email sent for shopper %s. Message id: %s", request.shopperId, messageId));
        } catch (Exception ex) {
            logger.error(
                    String.format("Failed sending setup email for shopper %s: %s", request.shopperId, ex.getMessage()),
                    ex);
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

    public static class Response {
        public long vmId;
    }

    public static class ActionState {
        public CreateVmStep step;
    }

}
