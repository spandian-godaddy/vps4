package com.godaddy.vps4.orchestration.vm;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp.BindIpRequest;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVmFromSnapshot;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.RestoreVmInfo;
import com.godaddy.vps4.vm.RestoreVmStep;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@CommandMetadata(
        name="Vps4RestoreVm",
        requestType=Vps4RestoreVm.Request.class,
        responseType=Vps4RestoreVm.Response.class
)
public class Vps4RestoreVm extends ActionCommand<Vps4RestoreVm.Request, Vps4RestoreVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RestoreVm.class);

    final VmService vmService;

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
    public Vps4RestoreVm(ActionService actionService, VmService vmService,
                         VirtualMachineService virtualMachineService, NetworkService vps4NetworkService,
                         SnapshotService vps4SnapshotService, VmUserService vmUserService) {
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

        Vm hfsVm = createVmFromSnapshot();
        long newHfsVmId = hfsVm.vmId;

        // Post creation reconfigure steps
        setRootUserPassword(newHfsVmId);
        configureAdminUser(newHfsVmId);
        updateHfsVmId(newHfsVmId);
        bindPublicIpAddress(newHfsVmId, ipAddresses);

        deleteOldVm(oldHfsVmId);

        logger.info("vm restore finished");
        return null;
    }

    private long getOldHfsVmId() {
        return context.execute("GetHfsVmId", ctx -> virtualMachineService.getVirtualMachine(vps4VmId).hfsVmId);
    }

    private List<IpAddress> getPublicIpAddresses() {
        return context.execute(
                "GetPublicIpAdresses", ctx -> vps4NetworkService.getVmIpAddresses(vps4VmId));
    }

    private void unbindPublicIpAddresses(List<IpAddress> ipAddresses) {
        setStep(RestoreVmStep.UnbindingIPAddress);
        for (IpAddress ipAddress: ipAddresses) {
            logger.info("Unbind public ip address {} from VM {}", ipAddress.ipAddress, vps4VmId);
            context.execute(String.format("UnbindIP-%d", ipAddress.ipAddressId), UnbindIp.class, ipAddress.ipAddressId);
        }
    }

    private String getNocfoxImageIdForSnapshot(UUID snapshotId) {
        return context.execute("GetNocfoxImageId", ctx -> vps4SnapshotService.getSnapshot(snapshotId).hfsImageId);
    }

    private String getVmOSDistro() {
        return context.execute("GetVmOSDistro", ctx -> virtualMachineService.getOSDistro(vps4VmId));
    }

    private CreateVMWithFlavorRequest createHfsRequest() {
        CreateVMWithFlavorRequest hfsProvisionRequest = new CreateVMWithFlavorRequest();
        hfsProvisionRequest.rawFlavor = request.restoreVmInfo.rawFlavor;
        hfsProvisionRequest.sgid = request.restoreVmInfo.sgid;
        hfsProvisionRequest.image_id = getNocfoxImageIdForSnapshot(vps4SnapshotId);
        hfsProvisionRequest.os = getVmOSDistro();
        hfsProvisionRequest.username = request.restoreVmInfo.username;
        hfsProvisionRequest.password = request.restoreVmInfo.password;
        hfsProvisionRequest.zone = request.restoreVmInfo.zone;
        hfsProvisionRequest.hostname = request.restoreVmInfo.hostname;
        hfsProvisionRequest.ignore_whitelist = "True";
        return hfsProvisionRequest;
    }

    private Vm createVmFromSnapshot() {
        setStep(RestoreVmStep.RequestingServer);
        logger.info("create vm from snapshot");

        CreateVMWithFlavorRequest hfsRequest = createHfsRequest();
        VmAction vmAction = context.execute("CreateVmFromSnapshot", CreateVmFromSnapshot.class, hfsRequest);

        // Get the hfs vm
        return context.execute("GetVmAfterCreate", ctx -> vmService.getVm(vmAction.vmId));
    }

    private void updateHfsVmId(long hfsVmId) {
        context.execute("UpdateHfsVmId", ctx -> {
            virtualMachineService.addHfsVmIdToVirtualMachine(vps4VmId, hfsVmId);
            return null;
        });
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
        setPasswordRequest.password = request.restoreVmInfo.password;
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
        });
    }

    private void bindPublicIpAddress(long hfsVmId, List<IpAddress> ipAddresses) {
        setStep(RestoreVmStep.ConfiguringNetwork);
        logger.info("Bind public ip address");

        for (IpAddress ipAddress: ipAddresses) {
            logger.info("Bind public ip address {} to VM {}", ipAddress.ipAddress, vps4VmId);

            BindIpRequest bindRequest = new BindIpRequest();
            bindRequest.addressId = ipAddress.ipAddressId;
            bindRequest.vmId = hfsVmId;
            context.execute(String.format("BindIP-%d", ipAddress.ipAddressId), BindIp.class, bindRequest);
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
