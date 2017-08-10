package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateCreditIsNotInUse;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsActive;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsStopped;
import static com.godaddy.vps4.web.util.RequestValidation.validateUserIsShopper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.orchestration.vm.Vps4ProvisionVm;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.ProvisionVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.VirtualMachineSpec;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.Commands;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmResource {

    private static final Logger logger = LoggerFactory.getLogger(VmResource.class);

    private final GDUser user;
    private final VirtualMachineService virtualMachineService;
    private final Vps4UserService userService;
    private final CreditService creditService;
    private final VmService vmService;
    private final ProjectService projectService;
    private final ImageService imageService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final Config config;
    private final String sgidPrefix;
    private final int mailRelayQuota;
    private final long pingCheckAccountId;

    @Inject
    public VmResource(GDUser user, VmService vmService,
            Vps4UserService userService,
            VirtualMachineService virtualMachineService,
            CreditService creditService,
            ProjectService projectService,
            ImageService imageService,
            ActionService actionService,
            CommandService commandService,
            Config config) {

        this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.userService = userService;
        this.creditService = creditService;
        this.vmService = vmService;
        this.projectService = projectService;
        this.imageService = imageService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.config = config;
        sgidPrefix = this.config.get("hfs.sgid.prefix", "vps4-undefined-");
        mailRelayQuota = Integer.parseInt(this.config.get("mailrelay.quota", "5000"));
        pingCheckAccountId = Long.parseLong(this.config.get("nodeping.accountid"));
    }

    @GET
    @Path("/{vmId}")
    public VirtualMachine getVm(@PathParam("vmId") UUID vmId) {
        logger.info("getting vm with id {}", vmId);
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);

        if (virtualMachine == null || virtualMachine.validUntil.isBefore(Instant.now()))
            throw new NotFoundException("Unknown VM ID: " + vmId);

        logger.info(String.format("VM valid until: %s | %s", virtualMachine.validUntil, Instant.now()));
        if (user.isShopper())
            getAndValidateUserAccountCredit(creditService, virtualMachine.orionGuid, user.getShopperId());

        return virtualMachine;
    }

    private VmAction createActionAndExecute(UUID vmId, ActionType actionType, VmActionRequest request, String commandName) {
        long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);
        long actionId = actionService.createAction(vmId, actionType, new JSONObject().toJSONString(), vps4UserId);
        request.setActionId(actionId);

        CommandState command = Commands.execute(commandService, actionService, commandName, request);
        logger.info("managing vm {} with command {}:{}", vmId, actionType, command.commandId);
        return new VmAction(actionService.getAction(actionId));
    }

    @POST
    @Path("{vmId}/start")
    public VmAction startVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);
        validateNoConflictingActions(vmId, actionService,
                ActionType.START_VM, ActionType.STOP_VM, ActionType.RESTART_VM);
        validateServerIsStopped(vmService.getVm(vm.hfsVmId));

        VmActionRequest startRequest = new VmActionRequest();
        startRequest.hfsVmId = vm.hfsVmId;
        return createActionAndExecute(vm.vmId, ActionType.START_VM, startRequest, "Vps4StartVm");
    }

    @POST
    @Path("{vmId}/stop")
    public VmAction stopVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);
        validateNoConflictingActions(vmId, actionService,
                ActionType.START_VM, ActionType.STOP_VM, ActionType.RESTART_VM);
        validateServerIsActive(vmService.getVm(vm.hfsVmId));

        VmActionRequest stopRequest = new VmActionRequest();
        stopRequest.hfsVmId = vm.hfsVmId;
        return createActionAndExecute(vm.vmId, ActionType.STOP_VM, stopRequest, "Vps4StopVm");
    }

    @POST
    @Path("{vmId}/restart")
    public VmAction restartVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);
        validateNoConflictingActions(vmId, actionService,
                ActionType.START_VM, ActionType.STOP_VM, ActionType.RESTART_VM);
        validateServerIsActive(vmService.getVm(vm.hfsVmId));

        VmActionRequest restartRequest = new VmActionRequest();
        restartRequest.hfsVmId = vm.hfsVmId;
        return createActionAndExecute(vm.vmId, ActionType.RESTART_VM, restartRequest, "Vps4RestartVm");
    }

    public static class ProvisionVmRequest {
        public String name;
        public UUID orionGuid;
        public String image;
        public int dataCenterId;
        public String username;
        public String password;
    }

    @POST
    @Path("/")
    public VmAction provisionVm(ProvisionVmRequest provisionRequest) throws InterruptedException {
        logger.info("provisioning vm with orionGuid {}", provisionRequest.orionGuid);

        validateUserIsShopper(user);
        VirtualMachineCredit vmCredit = getAndValidateUserAccountCredit(creditService,
                provisionRequest.orionGuid, user.getShopperId());
        validateCreditIsNotInUse(vmCredit);

        if(imageService.getImages(vmCredit.operatingSystem, vmCredit.controlPanel, provisionRequest.image).size() == 0){
            // verify that the image matches the request (control panel, managed level, OS)
            String message = String.format("The image %s is not valid for this credit.", provisionRequest.image);
            throw new Vps4Exception("INVALID_IMAGE", message);
        }

        ProvisionVirtualMachineParameters params;
        VirtualMachine virtualMachine;
        Vps4User vps4User = userService.getOrCreateUserForShopper(user.getShopperId());
        try {
            params = new ProvisionVirtualMachineParameters(vps4User.getId(), provisionRequest.dataCenterId,
                    sgidPrefix, provisionRequest.orionGuid, provisionRequest.name, vmCredit.tier, vmCredit.managedLevel,
                    provisionRequest.image);
            virtualMachine = virtualMachineService.provisionVirtualMachine(params);
            creditService.claimVirtualMachineCredit(provisionRequest.orionGuid, provisionRequest.dataCenterId, virtualMachine.vmId);
        }
        catch (Exception e) {
            throw new Vps4Exception("PROVISION_VM_FAILED", e.getMessage(), e);
        }

        Project project = projectService.getProject(virtualMachine.projectId);

        CreateVMWithFlavorRequest hfsRequest = createHfsProvisionVmRequest(provisionRequest.image, provisionRequest.username,
                provisionRequest.password, project, virtualMachine.spec);

        long actionId = actionService.createAction(virtualMachine.vmId, ActionType.CREATE_VM, new JSONObject().toJSONString(),
                vps4User.getId());
        logger.info("VmAction id: {}", actionId);

        long ifMonitoringThenMonitoringAccountId = vmCredit.monitoring == 1 ? pingCheckAccountId : 0;

        ProvisionVmInfo vmInfo = new ProvisionVmInfo(virtualMachine.vmId, vmCredit.managedLevel, virtualMachine.image,
                project.getVhfsSgid(), mailRelayQuota, ifMonitoringThenMonitoringAccountId);
        logger.info("vmInfo: {}", vmInfo.toString());

        Vps4ProvisionVm.Request request = createProvisionVmRequest(hfsRequest, actionId, vmInfo);

        CommandState command = Commands.execute(commandService, actionService, "ProvisionVm", request);
        logger.info("provisioning VM in {}", command.commandId);

        return new VmAction(actionService.getAction(actionId));
    }

    private Vps4ProvisionVm.Request createProvisionVmRequest(CreateVMWithFlavorRequest hfsRequest,
                                                         long actionId,
                                                         ProvisionVmInfo vmInfo) {
        Vps4ProvisionVm.Request request = new Vps4ProvisionVm.Request();
        request.actionId = actionId;
        request.hfsRequest = hfsRequest;
        request.vmInfo = vmInfo;
        return request;
    }

    private CreateVMWithFlavorRequest createHfsProvisionVmRequest(String image, String username,
            String password, Project project, VirtualMachineSpec spec) {
        CreateVMWithFlavorRequest hfsProvisionRequest = new CreateVMWithFlavorRequest();
        hfsProvisionRequest.rawFlavor = spec.specName;
        hfsProvisionRequest.sgid = project.getVhfsSgid();
        hfsProvisionRequest.image_name = image;
        hfsProvisionRequest.username = username;
        hfsProvisionRequest.password = password;
        hfsProvisionRequest.zone = config.get("openstack.zone", null);
        return hfsProvisionRequest;
    }

    @DELETE
    @Path("/{vmId}")
    public VmAction destroyVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);
        validateNoConflictingActions(vmId, actionService,
                ActionType.START_VM, ActionType.STOP_VM, ActionType.RESTART_VM, ActionType.CREATE_VM);

        Vps4DestroyVm.Request destroyRequest = new Vps4DestroyVm.Request();
        destroyRequest.hfsVmId = vm.hfsVmId;
        destroyRequest.pingCheckAccountId = pingCheckAccountId;

        VmAction deleteAction = createActionAndExecute(vm.vmId, ActionType.DESTROY_VM, destroyRequest, "Vps4DestroyVm");

        // The request has been created successfully.
        // Detach the user from the vm, and we'll handle the delete from here.
        creditService.unclaimVirtualMachineCredit(vm.orionGuid);
        virtualMachineService.destroyVirtualMachine(vm.hfsVmId);

        return deleteAction;
    }

    @GET
    @Path("/")
    public List<VirtualMachine> getVirtualMachines() {
        if (user.getShopperId() == null)
            throw new Vps4NoShopperException();
        Vps4User vps4User = userService.getOrCreateUserForShopper(user.getShopperId());

        List<VirtualMachine> vms = virtualMachineService.getVirtualMachinesForUser(vps4User.getId());
        return vms.stream().filter(vm -> vm.validUntil.isAfter(Instant.now())).collect(Collectors.toList());
    }

    @GET
    @Path("/{vmId}/details")
    public VirtualMachineDetails getVirtualMachineDetails(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = getVm(vmId);
        Vm vm = getVmFromVmVertical(virtualMachine.hfsVmId);
        return new VirtualMachineDetails(vm);
    }

    @GET
    @Path("/{vmId}/hfsDetails")
    public Vm getMoreDetails(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = getVm(vmId);
        return getVmFromVmVertical(virtualMachine.hfsVmId);
    }

    public Vm getVmFromVmVertical(long vmId) {
        try {
            return vmService.getVm(vmId);
        }
        catch (Exception e) {
            logger.warn("Cannot find VM ID {} in vm vertical", vmId);
            return null;
        }
    }

    @GET
    @Path("/{vmId}/withDetails")
    public VirtualMachineWithDetails getVirtualMachineWithDetails(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = getVm(vmId);
        DataCenter dc = creditService.getVirtualMachineCredit(virtualMachine.orionGuid).dataCenter;
        Vm vm = getVmFromVmVertical(virtualMachine.hfsVmId);
        return new VirtualMachineWithDetails(virtualMachine, new VirtualMachineDetails(vm), dc);
    }
}
