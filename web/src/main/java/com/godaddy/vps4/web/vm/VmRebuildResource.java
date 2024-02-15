package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.sysadmin.UsernamePasswordGenerator.generatePassword;
import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validatePassword;
import static com.godaddy.vps4.web.util.RequestValidation.validateRequestedImage;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.PleskLicenseType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.vm.rebuild.Vps4RebuildVm;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.RebuildVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmRebuildResource {
    private static final Logger logger = LoggerFactory.getLogger(VmRebuildResource.class);

    private final GDUser user;
    private final VmUserService vmUserService;
    private final ProjectService projectService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final ImageService imageService;
    private final SnapshotService snapshotService;
    private final CreditService creditService;
    private final VmResource vmResource;
    private final VmActionResource vmActionResource;
    private final VmSnapshotResource vmSnapshotResource;
    private final ImageResource imageResource;
    private final Config config;
    private final Cryptography cryptography;
    private final int MAX_PASSWORD_LENGTH = 48;

    @Inject
    public VmRebuildResource(
            GDUser user,
            VmUserService vmUserService,
            ProjectService projectService,
            ActionService actionService,
            CommandService commandService,
            ImageService imageService,
            SnapshotService snapshotService,
            CreditService creditService,
            VmResource vmResource,
            VmActionResource vmActionResource,
            VmSnapshotResource vmSnapshotResource,
            Config config,
            Cryptography cryptography,
            ImageResource imageResource
    ) {
        this.user = user;
        this.vmUserService = vmUserService;
        this.projectService = projectService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.imageService = imageService;
        this.snapshotService = snapshotService;
        this.creditService = creditService;
        this.vmResource = vmResource;
        this.vmActionResource = vmActionResource;
        this.vmSnapshotResource = vmSnapshotResource;
        this.config = config;
        this.cryptography = cryptography;
        this.imageResource = imageResource;
    }

    public static class RebuildVmRequest {
        public String username;
        public String hostname;
        public String serverName;
        public String imageName;
        public String password;
        public boolean keepAdditionalIps = true;
    }

    private String getCustomerJwt() {
        if (user.isShopper() && !user.isShopperInjected()) {
            return user.getToken().getJwt().getParsedString();
        }
        return null;
    }

    @POST
    @Path("{vmId}/rebuild")
    public VmAction rebuild(@PathParam("vmId") UUID vmId, RebuildVmRequest rebuildVmRequest) {
        performAdminPrereqs(rebuildVmRequest);
        VirtualMachine vm = vmResource.getVm(vmId);
        isValidRebuildVmRequest(vmId, rebuildVmRequest,vm.orionGuid);
        logger.info("Processing rebuild on VM {}", vmId);

        cancelIncompleteVmActions(vmId);
        if (vm.spec.isVirtualMachine()) {
            destroyVmSnapshots(vmId);
        }

        Vps4RebuildVm.Request commandRequest = generateRebuildVmOrchestrationRequest(vm, rebuildVmRequest);
        String rebuildClassName = vm.spec.serverType.platform.getRebuildCommand();
        return createActionAndExecute(actionService, commandService, vm.vmId, ActionType.REBUILD_VM, commandRequest, rebuildClassName, user);
    }

    private void performAdminPrereqs(RebuildVmRequest rebuildVmRequest) {
        if (user.isAdmin()) {
            if (StringUtils.isBlank(rebuildVmRequest.password)) {
                rebuildVmRequest.password = generatePassword(MAX_PASSWORD_LENGTH);
            }
        }
    }

    private void isValidRebuildVmRequest(UUID vmId, RebuildVmRequest rebuildVmRequest, UUID orionGuid) {
        validateNoConflictingActions(vmId, actionService, ActionType.RESTORE_VM, ActionType.CREATE_VM, ActionType.REBUILD_VM);
        validatePassword(rebuildVmRequest.password);
        validateRequestedImage(creditService.getVirtualMachineCredit(orionGuid),imageResource.getImage(rebuildVmRequest.imageName));
        if (!StringUtils.isBlank(rebuildVmRequest.imageName)) {
            // Validate image name passed in. This should throw a 404 if image is disabled (for env or role)
            imageResource.getImage(rebuildVmRequest.imageName);
        }
    }

    private Vps4RebuildVm.Request generateRebuildVmOrchestrationRequest(
            VirtualMachine vm, RebuildVmRequest request) {

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        RebuildVmInfo rebuildVmInfo = new RebuildVmInfo();
        rebuildVmInfo.hostname = StringUtils.isBlank(request.hostname) ? vm.hostname : request.hostname;
        rebuildVmInfo.serverName = StringUtils.isBlank(request.serverName) ? vm.name : request.serverName;
        rebuildVmInfo.encryptedPassword = cryptography.encrypt(request.password);
        rebuildVmInfo.customerId = credit.getCustomerId();
        rebuildVmInfo.rawFlavor = vm.spec.specName;
        rebuildVmInfo.sgid = projectService.getProject(vm.projectId).getVhfsSgid();
        rebuildVmInfo.username = StringUtils.isBlank(request.username) ? vmUserService.getPrimaryCustomer(vm.vmId).username : request.username;
        rebuildVmInfo.vmId = vm.vmId;
        rebuildVmInfo.image = StringUtils.isBlank(request.imageName) ? vm.image : imageService.getImageByHfsName(request.imageName);
        if(rebuildVmInfo.image.hasPlesk()) {
            rebuildVmInfo.pleskLicenseType = validateAndReturnEnumValue(PleskLicenseType.class, credit.getControlPanel());
        }
        rebuildVmInfo.ipAddress = vm.primaryIpAddress;
        rebuildVmInfo.zone = vm.spec.isVirtualMachine() ?
                config.get(vm.dataCenter.dataCenterName + ".openstack.zone", null) :
                config.get(vm.dataCenter.dataCenterName + ".ovh.zone", null);
        rebuildVmInfo.privateLabelId = credit.getResellerId();
        rebuildVmInfo.orionGuid = vm.orionGuid;
        rebuildVmInfo.shopperId = user.isShopper() ? user.getShopperId(): creditService.getVirtualMachineCredit(vm.orionGuid).getShopperId();
        rebuildVmInfo.keepAdditionalIps = request.keepAdditionalIps;
        rebuildVmInfo.gdUserName = user.getUsername();
        rebuildVmInfo.isManaged = credit.isManaged();
        Vps4RebuildVm.Request req = new Vps4RebuildVm.Request();
        req.rebuildVmInfo = rebuildVmInfo;
        return req;
    }

    private void cancelIncompleteVmActions(UUID vmId) {
        logger.info("Cancelling any incomplete actions on VM {}", vmId);
        List<Action> actions = actionService.getIncompleteActions(vmId);
        for (Action action: actions) {
            vmActionResource.cancelVmAction(vmId, action.id);
        }
    }

    private void destroyVmSnapshots(UUID vmId) {
        List<Snapshot> snapshots = vmSnapshotResource.getSnapshotsForVM(vmId);
        for (Snapshot snapshot : snapshots) {
            if (snapshot.status == SnapshotStatus.NEW
                    || snapshot.status == SnapshotStatus.ERROR
                    || snapshot.status == SnapshotStatus.ERROR_RESCHEDULED
                    || snapshot.status == SnapshotStatus.LIMIT_RESCHEDULED
                    || snapshot.status == SnapshotStatus.AGENT_DOWN) {
                // just mark snapshots as cancelled if they were new or errored
                snapshotService.updateSnapshotStatus(snapshot.id, SnapshotStatus.CANCELLED);
            } else {
                vmSnapshotResource.destroySnapshot(vmId, snapshot.id);
            }
        }
    }
}
