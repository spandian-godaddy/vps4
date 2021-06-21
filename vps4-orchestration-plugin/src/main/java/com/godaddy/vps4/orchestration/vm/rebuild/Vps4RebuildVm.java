package com.godaddy.vps4.orchestration.vm.rebuild;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel.ConfigureCpanelRequest;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
import com.godaddy.vps4.orchestration.vm.Vps4RemoveIp;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.RebuildVmInfo;
import com.godaddy.vps4.vm.RebuildVmStep;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4RebuildVm",
        requestType = Vps4RebuildVm.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RebuildVm extends ActionCommand<Vps4RebuildVm.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4RebuildVm.class);

    protected final VirtualMachineService virtualMachineService;
    protected final NetworkService vps4NetworkService;
    protected final VmUserService vmUserService;
    protected final CreditService creditService;
    protected final PanoptaDataService panoptaDataService;
    protected final HfsVmTrackingRecordService hfsVmTrackingRecordService;
    protected final NetworkService networkService;
    protected final ShopperNotesService shopperNotesService;

    protected CommandContext context;
    protected UUID vps4VmId;
    protected Request request;

    private ActionState state;

    @Inject
    public Vps4RebuildVm(ActionService actionService, VirtualMachineService virtualMachineService,
                         NetworkService vps4NetworkService, VmUserService vmUserService, CreditService creditService,
                         PanoptaDataService panoptaDataService, HfsVmTrackingRecordService hfsVmTrackingRecordService,
                         NetworkService networkService, ShopperNotesService shopperNotesService) {
        super(actionService);
        this.virtualMachineService = virtualMachineService;
        this.vps4NetworkService = vps4NetworkService;
        this.vmUserService = vmUserService;
        this.creditService = creditService;
        this.panoptaDataService = panoptaDataService;
        this.hfsVmTrackingRecordService = hfsVmTrackingRecordService;
        this.networkService = networkService;
        this.shopperNotesService = shopperNotesService;
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request) throws Exception {
        this.state = new ActionState();
        this.context = context;
        this.request = request;
        this.vps4VmId = request.rebuildVmInfo.vmId;

        writeShopperNote();
        deleteOldUsersInDb();

        long oldHfsVmId = context.execute("GetHfsVmId",
                                          ctx -> virtualMachineService.getVirtualMachine(vps4VmId).hfsVmId,
                                          long.class);

        if(!request.rebuildVmInfo.keepAdditionalIps)
        {
            getAndRemoveAdditionalIps(oldHfsVmId);
        }

        long newHfsVmId = rebuildServer(oldHfsVmId);

        configureControlPanel(newHfsVmId);
        configureNewUser(newHfsVmId);
        setRootUserPassword(newHfsVmId);
        configureMailRelay(newHfsVmId);
        configureMonitoring(newHfsVmId);
        setEcommCommonName(request.rebuildVmInfo.orionGuid, request.rebuildVmInfo.serverName);

        setStep(RebuildVmStep.RebuildComplete);
        logger.info("VM rebuild of vmId {} finished", vps4VmId);
        return null;
    }

    private void writeShopperNote() {
        try {
            String shopperNote = String.format("Server was rebuilt by %s. VM ID: %s. Credit ID: %s.",
                                               request.rebuildVmInfo.gdUserName, vps4VmId,
                                               request.rebuildVmInfo.orionGuid);
            shopperNotesService.processShopperMessage(vps4VmId, shopperNote);
        } catch (Exception ignored) {}
    }

    private void getAndRemoveAdditionalIps(long oldHfsId) {
        List<IpAddress> additionalIps;
        additionalIps = networkService.getVmSecondaryAddress(oldHfsId);
        if (additionalIps != null) {
            for (IpAddress ip : additionalIps) {
                context.execute("RemoveIp-" + ip.addressId, Vps4RemoveIp.class, ip);
                context.execute("MarkIpDeleted-" + ip.addressId, ctx -> {
                    networkService.destroyIpAddress(ip.addressId);
                    return null;
                }, Void.class);
            }
        }
    }

    protected void deleteOldUsersInDb() {
        for (VmUser user : vmUserService.listUsers(vps4VmId)) {
            vmUserService.deleteUser(user.username, vps4VmId);
        }
    }

    protected long rebuildServer(long oldHfsVmId) throws Exception {
        unbindIpAddresses();
        destroyOldVm(oldHfsVmId);
        long newHfsVmId = createVm();
        bindIpAddresses(newHfsVmId);
        return newHfsVmId;
    }

    private void unbindIpAddresses() {
        List<IpAddress> ipAddresses = vps4NetworkService.getVmIpAddresses(vps4VmId);
        setStep(RebuildVmStep.UnbindingIPAddress);
        for (IpAddress ipAddress : ipAddresses) {
            logger.info("Unbind public ip address {} from VM {}", ipAddress.ipAddress, vps4VmId);
            UnbindIp.Request unbindIpRequest = new UnbindIp.Request();
            unbindIpRequest.hfsAddressId = ipAddress.hfsAddressId;
            unbindIpRequest.forceIfVmInaccessible = true;
            context.execute(String.format("UnbindIP-%d", ipAddress.hfsAddressId), UnbindIp.class, unbindIpRequest);
        }
    }

    private void destroyOldVm(long oldHfsVmId) {
        setStep(RebuildVmStep.DeleteOldVm);
        logger.info("Deleting HFS VM {}", oldHfsVmId);
        DestroyVm.Request destroyVmRequest = new DestroyVm.Request();
        destroyVmRequest.hfsVmId = oldHfsVmId;
        destroyVmRequest.actionId = request.actionId;
        context.execute("DestroyVmHfs", DestroyVm.class, destroyVmRequest);
    }

    private long createVm() {
        setStep(RebuildVmStep.RequestingServer);
        logger.info("create vm for rebuild process");

        CreateVm.Request createVmRequest = createHfsVmRequest();
        VmAction vmAction = context.execute("CreateVm", CreateVm.class, createVmRequest);

        // ensure the vm row is updated in the vps4 DB with the hfs vm id from the vm action.
        // having the hfs vm id in the vps4 database helps to track down the VM
        // in the event of a HFS VM provisioning failure.
        updateHfsVmId(vmAction.vmId);
        updateServerDetails(request);

        context.execute(WaitForAndRecordVmAction.class, vmAction);
        updateHfsVmTrackingRecord(vmAction);

        return vmAction.vmId;
    }

    private CreateVm.Request createHfsVmRequest() {
        CreateVm.Request createVm = new CreateVm.Request();
        createVm.sgid = request.rebuildVmInfo.sgid;
        createVm.image_name = request.rebuildVmInfo.image.hfsName;
        createVm.rawFlavor = request.rebuildVmInfo.rawFlavor;
        createVm.username = request.rebuildVmInfo.username;
        createVm.zone = request.rebuildVmInfo.zone;
        createVm.encryptedPassword = request.rebuildVmInfo.encryptedPassword;
        createVm.hostname = request.rebuildVmInfo.hostname;
        createVm.privateLabelId = request.rebuildVmInfo.privateLabelId;
        createVm.vmId = request.rebuildVmInfo.vmId;
        createVm.orionGuid = request.rebuildVmInfo.orionGuid;
        return createVm;
    }

    private void updateHfsVmId(long hfsVmId) {
        context.execute("UpdateHfsVmId", ctx -> {
            virtualMachineService.addHfsVmIdToVirtualMachine(vps4VmId, hfsVmId);
            return null;
        }, Void.class);
    }

    private void bindIpAddresses(long hfsVmId) {
        setStep(RebuildVmStep.ConfiguringNetwork);

        List<IpAddress> ipAddresses = vps4NetworkService.getVmIpAddresses(vps4VmId);
        for (IpAddress ipAddress : ipAddresses) {
            logger.info("Bind public ip address {} to VM {}", ipAddress.ipAddress, vps4VmId);

            BindIp.Request bindRequest = new BindIp.Request();
            bindRequest.hfsAddressId = ipAddress.hfsAddressId;
            bindRequest.hfsVmId = hfsVmId;
            context.execute(String.format("BindIP-%d", ipAddress.hfsAddressId), BindIp.class, bindRequest);
        }
    }

    protected void configureControlPanel(long hfsVmId) {
        Image image = request.rebuildVmInfo.image;
        if (image.hasCpanel()) {
            setStep(RebuildVmStep.ConfiguringCPanel);
            ConfigureCpanelRequest cpanelRequest = createConfigureCpanelRequest(hfsVmId);
            context.execute(ConfigureCpanel.class, cpanelRequest);
        } else if (image.hasPlesk()) {
            setStep(RebuildVmStep.ConfiguringPlesk);
            ConfigurePleskRequest pleskRequest = createConfigurePleskRequest(hfsVmId);
            context.execute(ConfigurePlesk.class, pleskRequest);
        }
    }

    private ConfigureCpanelRequest createConfigureCpanelRequest(long hfsVmId) {
        return new ConfigureCpanelRequest(hfsVmId);
    }

    private ConfigurePleskRequest createConfigurePleskRequest(long hfsVmId) {
        return new ConfigurePleskRequest(hfsVmId, request.rebuildVmInfo.username,
                                         request.rebuildVmInfo.encryptedPassword);
    }

    protected void configureNewUser(long hfsVmId) {
        String username = request.rebuildVmInfo.username;
        boolean enableAdmin = !virtualMachineService.hasControlPanel(vps4VmId);
        if (enableAdmin) {
            enableAdminAccess(hfsVmId, username);
        }
        vmUserService.createUser(username, vps4VmId, enableAdmin);
    }

    private void enableAdminAccess(long hfsVmId, String username) {
        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.vmId = hfsVmId;
        toggleAdminRequest.username = username;
        toggleAdminRequest.enabled = true;
        context.execute("ConfigureAdminAccess", ToggleAdmin.class, toggleAdminRequest);
    }

    protected void setRootUserPassword(long hfsVmId) {
        // set the root password to the same as the user password (LINUX ONLY)
        if (virtualMachineService.isLinux(vps4VmId)) {
            SetPassword.Request setRootPasswordRequest = createSetRootPasswordRequest(hfsVmId);
            context.execute("SetRootUserPassword", SetPassword.class, setRootPasswordRequest);
        }
    }

    private SetPassword.Request createSetRootPasswordRequest(long hfsVmId) {
        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = hfsVmId;
        setPasswordRequest.usernames = Collections.singletonList("root");
        setPasswordRequest.encryptedPassword = request.rebuildVmInfo.encryptedPassword;
        setPasswordRequest.controlPanel = request.rebuildVmInfo.image.getImageControlPanel();
        return setPasswordRequest;
    }

    protected void configureMailRelay(long hfsVmId) {
        setStep(RebuildVmStep.ConfigureMailRelay);

        ConfigureMailRelayRequest configureMailRelayRequest =
                new ConfigureMailRelayRequest(hfsVmId, request.rebuildVmInfo.image.controlPanel);
        context.execute(ConfigureMailRelay.class, configureMailRelayRequest);
    }

    protected void configureMonitoring(long hfsVmId) {
        if (hasPanoptaMonitoring()) {
            setStep(RebuildVmStep.ConfigureMonitoring);
            SetupPanopta.Request setupPanoptaRequest = new SetupPanopta.Request();
            setupPanoptaRequest.hfsVmId = hfsVmId;
            setupPanoptaRequest.orionGuid = request.rebuildVmInfo.orionGuid;
            setupPanoptaRequest.vmId = request.rebuildVmInfo.vmId;
            setupPanoptaRequest.shopperId = request.rebuildVmInfo.shopperId;
            setupPanoptaRequest.fqdn = vps4NetworkService.getVmPrimaryAddress(vps4VmId).ipAddress;
            context.execute(SetupPanopta.class, setupPanoptaRequest);
        }
    }

    private boolean hasPanoptaMonitoring() {
        PanoptaServerDetails panoptaDetails = panoptaDataService.getPanoptaServerDetails(vps4VmId);
        return panoptaDetails != null;
    }

    // Sets the name customers see in MYA when launching into their server dashboard
    protected void setEcommCommonName(UUID orionGuid, String commonName) {
        context.execute("SetCommonName", ctx -> {
            creditService.setCommonName(orionGuid, commonName);
            return null;
        }, Void.class);
    }

    protected void updateHfsVmTrackingRecord(VmAction vmAction){
        context.execute("UpdateHfsVmTrackingRecord", ctx -> {
            hfsVmTrackingRecordService.setCreated(vmAction.vmId, request.actionId);
            return null;
        }, Void.class);
    }

    protected void updateServerDetails(Request request) {
        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("name", request.rebuildVmInfo.serverName);
        vmPatchMap.put("image_id", request.rebuildVmInfo.image.imageId);
        context.execute("UpdateVmDetails", ctx -> {
            virtualMachineService.updateVirtualMachine(this.vps4VmId, vmPatchMap);
            return null;
        }, Void.class);
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

    public static class ActionState {
        public RebuildVmStep step;
    }
}
