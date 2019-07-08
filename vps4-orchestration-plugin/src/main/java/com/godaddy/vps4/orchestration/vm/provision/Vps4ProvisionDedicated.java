package com.godaddy.vps4.orchestration.vm.provision;

import static com.godaddy.vps4.vm.CreateVmStep.ConfigureNodeping;
import static com.godaddy.vps4.vm.CreateVmStep.ConfiguringCPanel;
import static com.godaddy.vps4.vm.CreateVmStep.ConfiguringPlesk;
import static com.godaddy.vps4.vm.CreateVmStep.GeneratingHostname;
import static com.godaddy.vps4.vm.CreateVmStep.RequestingServer;
import static com.godaddy.vps4.vm.CreateVmStep.SetHostname;
import static com.godaddy.vps4.vm.CreateVmStep.SetupComplete;
import static com.godaddy.vps4.vm.CreateVmStep.StartingServerSetup;

import java.util.Random;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel.ConfigureCpanelRequest;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;
import com.godaddy.vps4.util.MonitoringMeta;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.CreateVmStep;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.nodeping.CreateCheckRequest;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;

@CommandMetadata(
        name = "ProvisionDedicated",
        requestType = ProvisionRequest.class,
        responseType = Vps4ProvisionDedicated.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ProvisionDedicated extends ActionCommand<ProvisionRequest, Vps4ProvisionDedicated.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ProvisionDedicated.class);
    private final VmService vmService;
    private final VirtualMachineService virtualMachineService;
    private final VmUserService vmUserService;
    private final NetworkService networkService;
    private final NodePingService monitoringService;
    private final MonitoringMeta monitoringMeta;
    private final Vps4MessagingService messagingService;
    private final CreditService creditService;

    private ProvisionRequest request;
    private ActionState state;
    private CommandContext context;

    @Inject
    public Vps4ProvisionDedicated(
            ActionService actionService,
            VmService vmService,
            VirtualMachineService virtualMachineService,
            VmUserService vmUserService,
            NetworkService networkService,
            NodePingService monitoringService,
            MonitoringMeta monitoringMeta,
            Vps4MessagingService messagingService,
            CreditService creditService) {
        super(actionService);
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.vmUserService = vmUserService;
        this.networkService = networkService;
        this.monitoringService = monitoringService;
        this.monitoringMeta = monitoringMeta;
        this.messagingService = messagingService;
        this.creditService = creditService;
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

        addIpToDb(hfsVm.address.ip_address);

        setupUsers(hfsVmId);

        configureControlPanel(hfsVmId);

        setHostname(hfsVmId, hfsVm.resource_id);

        configureAdminUser(hfsVmId, request.vmInfo.vmId);

        configureMonitoring(hfsVm.address.ip_address);

        setEcommCommonName(request.orionGuid, request.serverName);

        sendSetupEmail(request, hfsVm.address.ip_address);

        setStep(SetupComplete);
        logger.info("provision vm finished: {}", request.vmInfo.vmId);
        return null;
    }

    private void addIpToDb(String ipAddress){
        context.execute("Create-" + ipAddress, ctx -> {
            networkService.createIpAddress(0, request.vmInfo.vmId, ipAddress, IpAddressType.PRIMARY);
            return null;
        }, Void.class);
    }

    private void setHostname(long hfsVmId, String resourceId) {   
        setStep(SetHostname);
        SetHostname.Request hfsRequest = new SetHostname.Request(hfsVmId, resourceId, request.vmInfo.image.getImageControlPanel());
        virtualMachineService.setHostname(request.vmInfo.vmId, resourceId);
        context.execute(SetHostname.class, hfsRequest);
    }

    private void configureControlPanel(long hfsVmId) {
        if (request.vmInfo.image.hasCpanel()) {
            // VM with cPanel
            setStep(ConfiguringCPanel);

            // configure cpanel on the server
            ConfigureCpanelRequest cpanelRequest = createConfigureCpanelRequest(hfsVmId);
            context.execute(ConfigureCpanel.class, cpanelRequest);

        } else if (request.vmInfo.image.hasPlesk()) {
            // VM with Plesk image
            setStep(ConfiguringPlesk);

            // configure Plesk on the server
            ConfigurePleskRequest pleskRequest = createConfigurePleskRequest(hfsVmId);
            context.execute(ConfigurePlesk.class, pleskRequest);
        }
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
        setStep(RequestingServer);
        String hostname = "dedtemp.secureserver.net";
        CreateVm.Request createVmRequest = ProvisionHelper.getCreateVmRequest(request, hostname);
        VmAction vmAction = context.execute(CreateVm.class, createVmRequest);
        addHfsVmIdToVmInVps4Db(vmAction);
        context.execute(WaitForAndRecordVmAction.class, vmAction);
        return vmAction.vmId;
    }

    private void addHfsVmIdToVmInVps4Db(VmAction vmAction) {
        context.execute("Vps4ProvisionVm", ctx -> {
            virtualMachineService.addHfsVmIdToVirtualMachine(request.vmInfo.vmId, vmAction.vmId);
            return null;
        }, Void.class);
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

    private void configureMonitoring(String ipAddress) {
        if (request.vmInfo.hasMonitoring) {
            setStep(ConfigureNodeping);
            CreateCheckRequest checkRequest = ProvisionHelper.getCreateCheckRequest(ipAddress, monitoringMeta);
            NodePingCheck check = monitoringService.createCheck(monitoringMeta.getAccountId(), checkRequest);
            logger.debug("CheckId: {}", check.checkId);
            addCheckIdToIp(ipAddress, check);
        }
    }

    private void addCheckIdToIp(String ipAddress, NodePingCheck check) {
        context.execute("AddCheckIdToIp-" + ipAddress, ctx -> {
            networkService.updateIpWithCheckId(ipAddress, check.checkId);
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
