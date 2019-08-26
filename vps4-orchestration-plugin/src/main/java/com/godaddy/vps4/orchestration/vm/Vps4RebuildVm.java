package com.godaddy.vps4.orchestration.vm;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.credit.CreditService;
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
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
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
        name="Vps4RebuildVm",
        requestType=Vps4RebuildVm.Request.class,
        responseType=Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RebuildVm extends ActionCommand<Vps4RebuildVm.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RebuildVm.class);
    private final VirtualMachineService virtualMachineService;
    private final NetworkService vps4NetworkService;
    private final VmUserService vmUserService;
    private final CreditService creditService;

    private Request request;
    private ActionState state;
    private CommandContext context;
    private UUID vps4VmId;

    @Inject
    public Vps4RebuildVm(ActionService actionService, VirtualMachineService virtualMachineService,
                         NetworkService vps4NetworkService, VmUserService vmUserService, CreditService creditService) {
        super(actionService);
        this.virtualMachineService = virtualMachineService;
        this.vps4NetworkService = vps4NetworkService;
        this.vmUserService = vmUserService;
        this.creditService = creditService;
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request) throws Exception {
        this.request = request;
        this.context = context;
        this.state = new ActionState();
        this.vps4VmId = request.rebuildVmInfo.vmId;

        prepareForRebuild();
        long hfsVmId = createVm();
        configureNewVm(hfsVmId);

        setStep(RebuildVmStep.RebuildComplete);
        logger.info("VM rebuild of vmId {} finished", vps4VmId);
        return null;
    }

    private void prepareForRebuild() {
        List<IpAddress> ipAddresses = getPublicIpAddresses();
        unbindPublicIpAddresses(ipAddresses);

        deleteVmInHfs(getOriginalHfsVmId());
        deleteVmUsersInDb(vps4VmId);
    }

    private List<IpAddress> getPublicIpAddresses() {
        return vps4NetworkService.getVmIpAddresses(vps4VmId);
    }

    private void unbindPublicIpAddresses(List<IpAddress> ipAddresses) {
        setStep(RebuildVmStep.UnbindingIPAddress);
        for (IpAddress ipAddress: ipAddresses) {
            logger.info("Unbind public ip address {} from VM {}", ipAddress.ipAddress, vps4VmId);
            UnbindIp.Request unbindIpRequest = new UnbindIp.Request();
            unbindIpRequest.addressId = ipAddress.ipAddressId;
            unbindIpRequest.forceIfVmInaccessible = true;
            context.execute(String.format("UnbindIP-%d", ipAddress.ipAddressId), UnbindIp.class, unbindIpRequest);
        }
    }

    private long getOriginalHfsVmId() {
        return context.execute("GetHfsVmId", ctx -> virtualMachineService.getVirtualMachine(vps4VmId).hfsVmId, long.class);
    }

    private void deleteVmInHfs(long hfsVmId) {
        setStep(RebuildVmStep.DeleteOldVm);
        logger.info("Deleting HFS VM {}", hfsVmId);
        DestroyVm.Request destroyVmRequest = new DestroyVm.Request();
        destroyVmRequest.hfsVmId = hfsVmId;
        context.execute("DestroyVmHfs", DestroyVm.class, destroyVmRequest);
    }

    private void deleteVmUsersInDb(UUID oldVmId) {
        for (VmUser user : vmUserService.listUsers(oldVmId)) {
            vmUserService.deleteUser(user.username, oldVmId);
        }
    }

    private long createVm() {
        setStep(RebuildVmStep.RequestingServer);
        logger.info("create vm for rebuild process");

        CreateVm.Request createVmRequest = createHfsVmRequest();
        VmAction vmAction = context.execute("CreateVm", CreateVm.class, createVmRequest);

        // ensure the vm row is updated in the vps4 DB with the hfs vm id from the vm action.
        // having the hfs vm id in the vps4 database helps to track down the VM
        // in the event of a HFS VM provisioning failure.
        // Post creation reconfigure steps
        updateHfsVmId(vmAction.vmId);
        updateServerDetails(request);

        context.execute(WaitForAndRecordVmAction.class, vmAction);

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

    private void updateServerDetails(Request request) {
        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("name", request.rebuildVmInfo.serverName);
        vmPatchMap.put("image_id", request.rebuildVmInfo.image.imageId);
        context.execute("UpdateVmDetails", ctx -> {
            virtualMachineService.updateVirtualMachine(this.vps4VmId, vmPatchMap);
            return null;
        }, Void.class);
    }

    private void configureNewVm(long newHfsVmId) {
        List<IpAddress> ipAddresses = getPublicIpAddresses();
        bindPublicIpAddress(newHfsVmId, ipAddresses);
        configureControlPanel(newHfsVmId);
        setHostname(newHfsVmId);
        configureVmUser(newHfsVmId);
        configureMailRelay(newHfsVmId);
        setEcommCommonName(request.rebuildVmInfo.orionGuid, request.rebuildVmInfo.serverName);
    }

    private void bindPublicIpAddress(long hfsVmId, List<IpAddress> ipAddresses) {
        setStep(RebuildVmStep.ConfiguringNetwork);

        for (IpAddress ipAddress: ipAddresses) {
            logger.info("Bind public ip address {} to VM {}", ipAddress.ipAddress, vps4VmId);

            BindIp.Request bindRequest = new BindIp.Request();
            bindRequest.addressId = ipAddress.ipAddressId;
            bindRequest.hfsVmId = hfsVmId;
            context.execute(String.format("BindIP-%d", ipAddress.ipAddressId), BindIp.class, bindRequest);
        }
    }

    private void configureControlPanel(long hfsVmId) {
        Image image = virtualMachineService.getVirtualMachine(vps4VmId).image;
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
        ConfigureCpanelRequest cpanelRequest = new ConfigureCpanelRequest();
        cpanelRequest.vmId = hfsVmId;
        return cpanelRequest;
    }

    private ConfigurePleskRequest createConfigurePleskRequest(long hfsVmId) {
        return new ConfigurePleskRequest(hfsVmId, request.rebuildVmInfo.username, request.rebuildVmInfo.encryptedPassword);
    }

    private void setHostname(long hfsVmId) {
        setStep(RebuildVmStep.SetHostname);

        SetHostname.Request hfsRequest = new SetHostname.Request(hfsVmId, request.rebuildVmInfo.hostname,
                request.rebuildVmInfo.image.getImageControlPanel());
        context.execute(SetHostname.class, hfsRequest);
    }

    private void configureVmUser(long hfsVmId) {
        boolean enableAdmin = !virtualMachineService.hasControlPanel(vps4VmId);
        if (enableAdmin) {
            enableAdminAccess(hfsVmId);
        }

        createVmUserInDb(request.rebuildVmInfo.username, vps4VmId, enableAdmin);
        setRootUserPassword(hfsVmId);
    }

    private void enableAdminAccess(long hfsVmId) {
        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.vmId = hfsVmId;
        toggleAdminRequest.username = request.rebuildVmInfo.username;
        toggleAdminRequest.enabled = true;
        context.execute("ConfigureAdminAccess", ToggleAdmin.class, toggleAdminRequest);
    }

    private void createVmUserInDb(String username, UUID newVmId, boolean enableAdmin) {
        vmUserService.createUser(username, newVmId, enableAdmin);
    }

    private void setRootUserPassword(long hfsVmId) {
        // set the root password to the same as the user password (LINUX ONLY)
        if(virtualMachineService.isLinux(vps4VmId)) {
            SetPassword.Request setRootPasswordRequest = createSetRootPasswordRequest(hfsVmId);
            context.execute("SetRootUserPassword", SetPassword.class, setRootPasswordRequest);
        }
    }

    private SetPassword.Request createSetRootPasswordRequest(long hfsVmId) {
        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = hfsVmId;
        setPasswordRequest.usernames = Arrays.asList("root");
        setPasswordRequest.encryptedPassword = request.rebuildVmInfo.encryptedPassword;
        setPasswordRequest.controlPanel = request.rebuildVmInfo.image.getImageControlPanel();
        return setPasswordRequest;
    }

    private void configureMailRelay(long hfsVmId) {
        setStep(RebuildVmStep.ConfigureMailRelay);

        ConfigureMailRelayRequest configureMailRelayRequest = new ConfigureMailRelayRequest(hfsVmId, request.rebuildVmInfo.image.controlPanel);
        context.execute(ConfigureMailRelay.class, configureMailRelayRequest);
    }

    // Sets the name customers see in MYA when launching into their server dashboard
    private void setEcommCommonName(UUID orionGuid, String commonName) {
        context.execute("SetCommonName", ctx -> {
            creditService.setCommonName(orionGuid, commonName);
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
