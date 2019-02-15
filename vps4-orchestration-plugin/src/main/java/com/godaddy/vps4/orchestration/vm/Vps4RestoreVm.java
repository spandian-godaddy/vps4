package com.godaddy.vps4.orchestration.vm;


import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.cpanel.RefreshCpanelLicense;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp.BindIpRequest;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVmFromSnapshot;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.RestoreVmInfo;
import com.godaddy.vps4.vm.RestoreVmStep;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4RestoreVm",
        requestType=Vps4RestoreVm.Request.class,
        responseType=Vps4RestoreVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RestoreVm extends ActionCommand<Vps4RestoreVm.Request, Vps4RestoreVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RestoreVm.class);
    private final VmService vmService;
    private final VirtualMachineService virtualMachineService;
    private final SnapshotService vps4SnapshotService;
    private final NetworkService vps4NetworkService;
    private final VmUserService vmUserService;
    private Request request;
    private ActionState state;
    private CommandContext context;
    private UUID vps4VmId;
    private UUID vps4SnapshotId;

    @Inject
    public Vps4RestoreVm(ActionService actionService, VmService vmService, VirtualMachineService virtualMachineService,
                         NetworkService vps4NetworkService, SnapshotService vps4SnapshotService, VmUserService vmUserService) {
        super(actionService);
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.vps4NetworkService = vps4NetworkService;
        this.vps4SnapshotService = vps4SnapshotService;
        this.vmUserService = vmUserService;
    }

    @Override
    public Response executeWithAction(CommandContext context, Request request) throws Exception {
        this.request = request;
        this.context = context;
        this.state = new ActionState();
        this.vps4VmId = request.restoreVmInfo.vmId;
        this.vps4SnapshotId = request.restoreVmInfo.snapshotId;

        long oldHfsVmId = getOldHfsVmId();
        List<IpAddress> ipAddresses = getPublicIpAddresses();
        unbindPublicIpAddresses(ipAddresses);

        Vm hfsVm;
        try {
            hfsVm = createVmFromSnapshot();
        } catch (RuntimeException e) {
            logger.info("Create VM failed during restore, binding ips back to vm {}", oldHfsVmId);
            // Since the IP was already previously bound to oldHfsVmId, send true to force the IP bind
            bindPublicIpAddress(oldHfsVmId, ipAddresses, true);
            throw e;
        }
        long newHfsVmId = hfsVm.vmId;

        // Post creation reconfigure steps
        updateHfsVmId(newHfsVmId);
        // The IP must be bound and configured on-box so do not force bind, set force to false.
        bindPublicIpAddress(newHfsVmId, ipAddresses, false);
        setRootUserPassword(newHfsVmId);
        configureAdminUser(newHfsVmId);
        refreshCpanelLicense();

        deleteOldVm(oldHfsVmId);

        logger.info("vm restore finished");
        return null;
    }

    private long getOldHfsVmId() {
        return context.execute("GetHfsVmId", ctx -> virtualMachineService.getVirtualMachine(vps4VmId).hfsVmId, long.class);
    }

    private Image getVmImageInfo() {
        return context.execute("GetImageInfo", ctx -> virtualMachineService.getVirtualMachine(vps4VmId).image, Image.class);
    }

    private List<IpAddress> getPublicIpAddresses() {
        return vps4NetworkService.getVmIpAddresses(vps4VmId);
    }

    private void unbindPublicIpAddresses(List<IpAddress> ipAddresses) {
        setStep(RestoreVmStep.UnbindingIPAddress);
        for (IpAddress ipAddress: ipAddresses) {
            logger.info("Unbind public ip address {} from VM {}", ipAddress.ipAddress, vps4VmId);
            UnbindIp.Request unbindIpRequest = new UnbindIp.Request();
            unbindIpRequest.addressId = ipAddress.ipAddressId;
            unbindIpRequest.forceIfVmInaccessible = true;
            context.execute(String.format("UnbindIP-%d", ipAddress.ipAddressId), UnbindIp.class, unbindIpRequest);
        }
    }

    private String getNocfoxImageIdForSnapshot(UUID snapshotId) {
        return context.execute("GetNocfoxImageId", ctx -> vps4SnapshotService.getSnapshot(snapshotId).hfsImageId, String.class);
    }

    private String getVmOSDistro() {
        return context.execute("GetVmOSDistro", ctx -> virtualMachineService.getOSDistro(vps4VmId), String.class);
    }

    private CreateVmFromSnapshot.Request createHfsRequest() {
        CreateVmFromSnapshot.Request createVmFromSnapshotRequest = new CreateVmFromSnapshot.Request();
        createVmFromSnapshotRequest.rawFlavor = request.restoreVmInfo.rawFlavor;
        createVmFromSnapshotRequest.sgid = request.restoreVmInfo.sgid;
        createVmFromSnapshotRequest.image_id = getNocfoxImageIdForSnapshot(vps4SnapshotId);
        createVmFromSnapshotRequest.os = getVmOSDistro();
        createVmFromSnapshotRequest.username = request.restoreVmInfo.username;
        createVmFromSnapshotRequest.encryptedPassword = request.restoreVmInfo.encryptedPassword;
        createVmFromSnapshotRequest.zone = request.restoreVmInfo.zone;
        createVmFromSnapshotRequest.hostname = request.restoreVmInfo.hostname;
        createVmFromSnapshotRequest.ignore_whitelist = "True";
        createVmFromSnapshotRequest.privateLabelId = request.privateLabelId;
        return createVmFromSnapshotRequest;
    }

    private Vm createVmFromSnapshot() {
        setStep(RestoreVmStep.RequestingServer);
        logger.info("create vm from snapshot");

        CreateVmFromSnapshot.Request createVmFromSnapshotRequest = createHfsRequest();
        VmAction vmAction = context.execute("CreateVmFromSnapshot", CreateVmFromSnapshot.class, createVmFromSnapshotRequest);

        // Get the hfs vm
        return context.execute("GetVmAfterCreate", ctx -> vmService.getVm(vmAction.vmId), Vm.class);
    }

    private void updateHfsVmId(long hfsVmId) {
        context.execute("UpdateHfsVmId", ctx -> {
            virtualMachineService.addHfsVmIdToVirtualMachine(vps4VmId, hfsVmId);
            return null;
        }, Void.class);
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
        String[] usernames = {"root"};
        setPasswordRequest.usernames = Arrays.asList(usernames);
        setPasswordRequest.encryptedPassword = request.restoreVmInfo.encryptedPassword;
        setPasswordRequest.controlPanel = getVmImageInfo().getImageControlPanel();
        return setPasswordRequest;
    }

    private void configureAdminUser(long hfsVmId) {
        boolean adminEnabled = !virtualMachineService.hasControlPanel(vps4VmId);
        String username = request.restoreVmInfo.username;
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

    private void bindPublicIpAddress(long hfsVmId, List<IpAddress> ipAddresses, boolean shouldForce) {
        setStep(RestoreVmStep.ConfiguringNetwork);
        logger.info("Bind public ip address");

        for (IpAddress ipAddress: ipAddresses) {
            logger.info("Bind public ip address {} to VM {}", ipAddress.ipAddress, vps4VmId);

            BindIpRequest bindRequest = new BindIpRequest();
            bindRequest.addressId = ipAddress.ipAddressId;
            bindRequest.vmId = hfsVmId;
            bindRequest.shouldForce = shouldForce;
            context.execute(String.format("BindIP-%d", ipAddress.ipAddressId), BindIp.class, bindRequest);
        }
    }

    private void refreshCpanelLicense(){
        VirtualMachine vm = context.execute("GetVirtualMachine",
                ctx -> virtualMachineService.getVirtualMachine(vps4VmId),
                VirtualMachine.class);

        if(vm.image.hasCpanel()){
            RefreshCpanelLicense.Request req = new RefreshCpanelLicense.Request();
            req.hfsVmId = vm.hfsVmId;
            context.execute("RefreshCPanelLicense", RefreshCpanelLicense.class, req);
        }

    }

    private void deleteOldVm(long hfsVmId) {
        setStep(RestoreVmStep.DeleteOldVm);
        logger.info("Delete old VM");
        context.execute("DestroyVmHfs", DestroyVm.class, hfsVmId);
    }

    protected void setStep(RestoreVmStep step) {
        state.step = step;

        try {
            actionService.updateActionState(request.getActionId(), mapper.writeValueAsString(state));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Request implements ActionRequest {
        public RestoreVmInfo restoreVmInfo;
        public String privateLabelId;
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
        public RestoreVmStep step;
    }
}
