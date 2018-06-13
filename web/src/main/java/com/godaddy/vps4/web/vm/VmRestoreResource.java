package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.sysadmin.UsernamePasswordGenerator.generatePassword;
import static com.godaddy.vps4.web.util.RequestValidation.validateIfSnapshotExists;
import static com.godaddy.vps4.web.util.RequestValidation.validateIfSnapshotFromVm;
import static com.godaddy.vps4.web.util.RequestValidation.validateIfSnapshotIsLive;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validatePassword;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.orchestration.vm.Vps4RestoreVm;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.RestoreVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmRestoreResource {
    private static final Logger logger = LoggerFactory.getLogger(VmRestoreResource.class);

    private final GDUser user;
    private final VirtualMachineService virtualMachineService;
    private final SnapshotService snapshotService;
    private final VmUserService vmUserService;
    private final ProjectService projectService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final VmResource vmResource;
    private final Config config;
    private final Cryptography cryptography;
    private final int MAX_PASSWORD_LENGTH = 14;

    @Inject
    public VmRestoreResource(
            GDUser user,
            VmUserService vmUserService,
            VirtualMachineService virtualMachineService,
            SnapshotService snapshotService,
            ProjectService projectService,
            ActionService actionService,
            CommandService commandService,
            VmResource vmResource,
            Config config,
            Cryptography cryptography
    ) {

        this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.snapshotService = snapshotService;
        this.vmUserService = vmUserService;
        this.projectService = projectService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.vmResource = vmResource;
        this.config = config;
        this.cryptography = cryptography;
    }

    public static class RestoreVmRequest {
        public UUID backupId;
        public String password;
    }

    @POST
    @Path("{vmId}/restore")
    public VmAction restore(@PathParam("vmId") UUID vmId, RestoreVmRequest restoreVmRequest) {
        restoreVmRequest = performAdminPrereqs(vmId, restoreVmRequest);
        VirtualMachine vm = vmResource.getVm(vmId);
        isValidRestoreVmRequest(vmId, restoreVmRequest);
        logger.info("Processing restore on VM {} using snapshot {}", vmId, restoreVmRequest.backupId);
        Vps4RestoreVm.Request commandRequest = generateRestoreVmOrchestrationRequest(
                vm, restoreVmRequest.backupId, restoreVmRequest.password);
        VmAction restoreAction = createActionAndExecute(
            actionService, commandService, virtualMachineService,
            vm.vmId, ActionType.RESTORE_VM, commandRequest, "Vps4RestoreVm", user);
        return restoreAction;
    }

    private RestoreVmRequest performAdminPrereqs(UUID vmId, RestoreVmRequest restoreVmRequest) {
        if(user.isAdmin()) {
            if (restoreVmRequest.backupId == null) {
                restoreVmRequest.backupId = findMostRecentActiveVmSnapshot(vmId);
            }
            if (StringUtils.isBlank(restoreVmRequest.password)) {
                restoreVmRequest.password = generatePassword(MAX_PASSWORD_LENGTH);
            }
        }
        return restoreVmRequest;
    }

    private void isValidRestoreVmRequest(UUID vmId, RestoreVmRequest restoreVmRequest) {
        validateNoConflictingActions(vmId, actionService, ActionType.START_VM, ActionType.STOP_VM,
                ActionType.RESTART_VM, ActionType.RESTORE_VM, ActionType.CREATE_VM, ActionType.RESTORE_VM);
        validateIfSnapshotExists(snapshotService, restoreVmRequest.backupId);
        validateIfSnapshotIsLive(snapshotService, restoreVmRequest.backupId);
        validateIfSnapshotFromVm(virtualMachineService, snapshotService, vmId, restoreVmRequest.backupId);
        validatePassword(restoreVmRequest.password);
    }

    private UUID findMostRecentActiveVmSnapshot(UUID vmId) {
        try {

            Snapshot recentSnapshot =  snapshotService.getSnapshotsForVm(vmId).stream().filter(snapshot -> snapshot.status == SnapshotStatus.LIVE).findFirst()
                    .orElseThrow(() -> new Vps4Exception("MISSING_SNAPSHOT_FOR_RESTORE", String.format("Could not find valid snapshot for vmId %s ", vmId)));
            return recentSnapshot.id;
        } catch (Vps4Exception vps4ex) {
            logger.error("Could not find valid snapshot for vmId {}", vmId);
            throw vps4ex;
        }
    }

    private Vps4RestoreVm.Request generateRestoreVmOrchestrationRequest(
            VirtualMachine vm, UUID snapshotId, String password) {
        RestoreVmInfo restoreVmInfo = new RestoreVmInfo();
        restoreVmInfo.hostname = vm.hostname;
        restoreVmInfo.encryptedPassword = cryptography.encrypt(password);
        restoreVmInfo.rawFlavor = vm.spec.specName;
        restoreVmInfo.sgid = projectService.getProject(vm.projectId).getVhfsSgid();
        restoreVmInfo.snapshotId = snapshotId;
        restoreVmInfo.username = vmUserService.getPrimaryCustomer(vm.vmId).username;
        restoreVmInfo.vmId = vm.vmId;
        restoreVmInfo.zone = config.get("openstack.zone", null);

        Vps4RestoreVm.Request req = new Vps4RestoreVm.Request();
        req.restoreVmInfo = restoreVmInfo;
        return req;
    }
}
