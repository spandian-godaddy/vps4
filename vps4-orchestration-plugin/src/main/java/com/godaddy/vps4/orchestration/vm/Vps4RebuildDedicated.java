package com.godaddy.vps4.orchestration.vm;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel.ConfigureCpanelRequest;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.RebuildDedicated;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.RebuildVmInfo;
import com.godaddy.vps4.vm.RebuildVmStep;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4RebuildDedicated",
        requestType= Vps4RebuildDedicated.Request.class,
        responseType= Vps4RebuildDedicated.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RebuildDedicated extends ActionCommand<Vps4RebuildDedicated.Request, Vps4RebuildDedicated.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RebuildDedicated.class);
    private final VmService vmService;
    private final VirtualMachineService virtualMachineService;
    private final NetworkService networkService;
    private final VmUserService vmUserService;
    private final CreditService creditService;
    private final PanoptaDataService panoptaDataService;
    private Request request;
    private ActionState state;
    private CommandContext context;
    private UUID vps4VmId;

    @Inject
    public Vps4RebuildDedicated(ActionService actionService, VmService vmService, VirtualMachineService virtualMachineService,
                                VmUserService vmUserService, CreditService creditService,
                                NetworkService networkService, PanoptaDataService panoptaDataService) {
        super(actionService);
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.vmUserService = vmUserService;
        this.creditService = creditService;
        this.networkService = networkService;
        this.panoptaDataService = panoptaDataService;
    }

    @Override
    public Response executeWithAction(CommandContext context, Request request) throws Exception {
        this.request = request;
        this.context = context;
        this.state = new ActionState();
        this.vps4VmId = request.rebuildVmInfo.vmId;

        long oldHfsVmId = getOldHfsVmId();
        // remove any support users on the old dedicated server (or in the db)
        removeSupportUsers(vps4VmId);

        Vm hfsVm;
        try {
            hfsVm = rebuildDedicated(oldHfsVmId, request);
        } catch (RuntimeException e) {
            logger.info("Rebuild Dedicated vm failed for dedicated vm id: {}", oldHfsVmId);
            throw e;
        }

        long newHfsVmId = hfsVm.vmId;

        if(newHfsVmId == 0) {
            throw new  Exception("HFS Vm ID is not available. Expecting HFS VM ID.");
        }

        VirtualMachine oldVm = virtualMachineService.getVirtualMachine(vps4VmId);

        updateVmUser(request.rebuildVmInfo.username, oldVm.vmId, request.rebuildVmInfo.vmId);
        setRootUserPassword(newHfsVmId);
        configureControlPanel(newHfsVmId);
        configureAdminUser(newHfsVmId);

        updateServerDetails(request);
        configureMonitoring(newHfsVmId);
        setEcommCommonName(oldVm.orionGuid, request.rebuildVmInfo.serverName);

        setStep(RebuildVmStep.RebuildComplete);

        logger.info("Completed Dedicated VM Rebuild.");
        return null;
    }

    private void updateServerDetails(Request request) {
        // Update the servers name and the image.
        // Self managed customers can provision several different images
        String serverName = request.rebuildVmInfo.serverName;
        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("name", serverName);
        vmPatchMap.put("image_id", request.rebuildVmInfo.image.imageId);
        virtualMachineService.updateVirtualMachine(this.vps4VmId, vmPatchMap);
    }

    private long getOldHfsVmId() {
        return context.execute("GetHfsVmId", ctx -> virtualMachineService.getVirtualMachine(vps4VmId).hfsVmId, long.class);
    }

    private Vm rebuildDedicated(long oldHfsVmId, Request request) {
        setStep(RebuildVmStep.RequestingServer);
        logger.info("rebuild Dedicated vm process");

        RebuildDedicated.Request rebuildDedRequest = new RebuildDedicated.Request();
        rebuildDedRequest.vmId = oldHfsVmId;
        rebuildDedRequest.hostname = request.rebuildVmInfo.hostname;
        rebuildDedRequest.image_name = request.rebuildVmInfo.image.hfsName;
        rebuildDedRequest.username = request.rebuildVmInfo.username;
        rebuildDedRequest.encryptedPassword = request.rebuildVmInfo.encryptedPassword;
        VmAction vmAction = context.execute("RebuildDedicated", RebuildDedicated.class, rebuildDedRequest);

        // Get the hfs vm
        return context.execute("GetVmAfterCreate", ctx -> vmService.getVm(vmAction.vmId), Vm.class);
    }

    private void updateVmUser(String username, UUID oldVmId, UUID newVmId) {
        vmUserService.listUsers(oldVmId, VmUserType.CUSTOMER).stream()
                .forEach(user -> vmUserService.deleteUser(user.username, oldVmId));
        vmUserService.createUser(username, newVmId);
    }

    private void setRootUserPassword(long hfsVmId) {
        // set the root password to the same as the user password (LINUX ONLY)
        if(Image.OperatingSystem.LINUX == request.rebuildVmInfo.image.operatingSystem) {
            SetPassword.Request setRootPasswordRequest = createSetRootPasswordRequest(hfsVmId);
            context.execute("SetRootUserPassword", SetPassword.class, setRootPasswordRequest);
        }
    }

    private SetPassword.Request createSetRootPasswordRequest(long hfsVmId) {
        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = hfsVmId;
        String[] usernames = {"root"};
        setPasswordRequest.usernames = Arrays.asList(usernames);
        setPasswordRequest.encryptedPassword = request.rebuildVmInfo.encryptedPassword;
        setPasswordRequest.controlPanel = request.rebuildVmInfo.image.getImageControlPanel();
        return setPasswordRequest;
    }

    private void configureAdminUser(long hfsVmId) {
        boolean adminEnabled = !(doesRequestImageHaveControlPanel());
        String username = request.rebuildVmInfo.username;
        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.enabled = adminEnabled;
        toggleAdminRequest.vmId = hfsVmId;
        toggleAdminRequest.username = username;

        context.execute("ConfigureAdminAccess", ToggleAdmin.class, toggleAdminRequest);
        context.execute("UpdateVps4AdminUser", ctx -> {
            vmUserService.updateUserAdminAccess(username, vps4VmId, adminEnabled);
            return null;
        }, Void.class);
    }

    private boolean doesRequestImageHaveControlPanel() {
        return request.rebuildVmInfo.image.hasPaidControlPanel();
    }

    private void configureControlPanel(long hfsVmId) {
        if (request.rebuildVmInfo.image.hasCpanel()) {
            // VM with cPanel
            setStep(RebuildVmStep.ConfiguringCPanel);

            // configure cpanel on the vm
            ConfigureCpanelRequest cpanelRequest = createConfigureCpanelRequest(hfsVmId);
            context.execute(ConfigureCpanel.class, cpanelRequest);

        } else if (request.rebuildVmInfo.image.hasPlesk()) {
            // VM with Plesk image
            setStep(RebuildVmStep.ConfiguringPlesk);

            // configure Plesk on the vm
            ConfigurePleskRequest pleskRequest = createConfigurePleskRequest(hfsVmId);
            context.execute(ConfigurePlesk.class, pleskRequest);
        }
    }

    private ConfigureCpanelRequest createConfigureCpanelRequest(long vmId) {
        ConfigureCpanelRequest cpanelRequest = new ConfigureCpanelRequest();
        cpanelRequest.vmId = vmId;
        return cpanelRequest;
    }

    private ConfigurePleskRequest createConfigurePleskRequest(long hfsVmId) {
        return new ConfigurePleskRequest(hfsVmId, request.rebuildVmInfo.username, request.rebuildVmInfo.encryptedPassword);
    }

    private void configureMonitoring(long hfsVmId) {
        if (hasPanoptaMonitoring()) {
            setStep(RebuildVmStep.ConfigureMonitoring);
            SetupPanopta.Request setupPanoptaRequest = new SetupPanopta.Request();
            setupPanoptaRequest.hfsVmId = hfsVmId;
            setupPanoptaRequest.orionGuid = request.rebuildVmInfo.orionGuid;
            setupPanoptaRequest.vmId = request.rebuildVmInfo.vmId;
            setupPanoptaRequest.shopperId = request.rebuildVmInfo.shopperId;
            setupPanoptaRequest.fqdn = networkService.getVmPrimaryAddress(vps4VmId).ipAddress;
            context.execute(SetupPanopta.class, setupPanoptaRequest);
        }
    }

    private boolean hasPanoptaMonitoring() {
        PanoptaServerDetails panoptaDetails = panoptaDataService.getPanoptaServerDetails(vps4VmId);
        return panoptaDetails != null;
    }

    private void setEcommCommonName(UUID orionGuid, String commonName) {
        context.execute("SetCommonName", ctx -> {
            creditService.setCommonName(orionGuid, commonName);
            return null;
        }, Void.class);
    }

    private void removeSupportUsers(UUID vps4VmId) {
        List<VmUser> users = vmUserService.listUsers(vps4VmId, VmUserType.SUPPORT);
        users.forEach(user -> vmUserService.deleteUser(user.username, vps4VmId));
    }

    protected void setStep(RebuildVmStep step) {
        state.step = step;

        try {
            actionService.updateActionState(request.getActionId(), mapper.writeValueAsString(state));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Request implements ActionRequest {
        public RebuildVmInfo rebuildVmInfo;
        public long actionId;

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
        public RebuildVmStep step;
    }
}
