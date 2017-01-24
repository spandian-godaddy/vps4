package com.godaddy.vps4.web.vm;

import java.time.Instant;
import java.util.ArrayList;
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
import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.orchestration.vm.ProvisionVm;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.orchestration.vm.Vps4RestartVm;
import com.godaddy.vps4.orchestration.vm.Vps4StartVm;
import com.godaddy.vps4.orchestration.vm.Vps4StopVm;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.ProvisionVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineSpec;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.util.Commands;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.Flavor;
import gdg.hfs.vhfs.vm.FlavorList;
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

    private final Vps4User user;
    private final VirtualMachineService virtualMachineService;
    private final PrivilegeService privilegeService;
    private final VmService vmService;
    private final ProjectService projectService;
    private final ImageService imageService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final Config config;
    private final String sgidPrefix;

    @Inject
    public VmResource(PrivilegeService privilegeService,
            Vps4User user, VmService vmService,
            VirtualMachineService virtualMachineService,
            ProjectService projectService, ImageService imageService,
            com.godaddy.vps4.network.NetworkService vps4NetworkService,
            CPanelService cPanelService,
            ActionService actionService,
            CommandService commandService,
            Config config) {

        this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.privilegeService = privilegeService;
        this.vmService = vmService;
        this.projectService = projectService;
        this.imageService = imageService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.config = config;
        sgidPrefix = config.get("hfs.sgid.prefix", "vps4-undefined-");
    }


//    @GET
//    @Path("actions/provision/{orionGuid}")
//    public Action getProvisionActions(@PathParam("orionGuid") UUID orionGuid) {
//
//        Action action = getActionFromOrionGuid(orionGuid);
//        if (action == null) {
//            throw new NotFoundException("action or orionGuid " + orionGuid + " not found");
//        }
//
//        return action;
//    }
//
//    @GET
//    @Path("/provisions/{orionGuid}")
//    public Action getProvisionAction(@PathParam("orionGuid") UUID orionGuid) {
//
//        return getActionFromOrionGuid(orionGuid);
//    }
//
//    protected Action getActionFromOrionGuid(UUID orionGuid) {
//
//        // TODO
//        // - add an action_id to the provision request table
//        // - update that column when a provision is started for a specific provision request
//        // - add a way to look that up through actionService (or something)
//        return null;
//    }

    @GET
    @Path("/flavors")
    public List<Flavor> getFlavors() {

        logger.info("getting flavors from HFS...");

        FlavorList flavorList = vmService.listFlavors();
        logger.info("flavorList: {}", flavorList);
        if (flavorList != null && flavorList.results != null) {
            return flavorList.results;
        }
        return new ArrayList<>();
    }

    @GET
    @Path("/{vmId}")
    public VirtualMachine getVm(@PathParam("vmId") UUID vmId) {

        logger.info("getting vm with id {}", vmId);

        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);

        if (virtualMachine == null || virtualMachine.validUntil.isBefore(Instant.now())) {
            throw new NotFoundException("Unknown VM ID: " + vmId);
        }

        try {
            privilegeService.requireAnyPrivilegeToProjectId(user, virtualMachine.projectId);
        }
        catch (AuthorizationException e) {
            logger.warn("User {} not authorized for vmId {}. Rethrowing NotFoundException to prevent attempts to find valid VM ids.",
                    user.getShopperId(), vmId);
            throw new NotFoundException("Unknown VM ID: " + vmId);
        }

        return virtualMachine;
    }



//    @POST
//    @Path("/")
//    public VirtualMachineRequest createVm(@QueryParam("orionGuid") UUID orionGuid,
//            @QueryParam("operatingSystem") String operatingSystem,
//            @QueryParam("tier") int tier,
//            @QueryParam("controlPanel") String controlPanel,
//            @QueryParam("managedLevel") int managedLevel,
//            @QueryParam("shopperId") String shopperId) {
//
//        if (!user.getShopperId().equals(shopperId)) {
//            throw new AuthorizationException(user.getShopperId() + " can not create a vm with a different shopperId(" + shopperId + ")");
//        }
//
//        logger.info("creating new vm request for orionGuid {}", orionGuid);
//        virtualMachineService.createVirtualMachineRequest(orionGuid, operatingSystem, controlPanel, tier, managedLevel, shopperId);
//        return virtualMachineService.getVirtualMachineRequest(orionGuid);
//
//    }

    @POST
    @Path("{vmId}/start")
    public Action startVm(@PathParam("vmId") UUID vmId) throws VmNotFoundException {
        return manageVm(vmId, ActionType.START_VM);
    }

    @POST
    @Path("{vmId}/stop")
    public Action stopVm(@PathParam("vmId") UUID vmId) throws VmNotFoundException {
        return manageVm(vmId, ActionType.STOP_VM);
    }

    @POST
    @Path("{vmId}/restart")
    public Action restartVm(@PathParam("vmId") UUID vmId) throws VmNotFoundException {
        return manageVm(vmId, ActionType.RESTART_VM);
    }

    /**
     * Manage the vm to perform actions like start / stop / restart vm.
     *
     * @param vmId
     * @param action
     */
    private Action manageVm(UUID vmId, ActionType type) throws VmNotFoundException {

        // Check if vm exists and user has access to the vm.
        VirtualMachine vm = getVm(vmId);
        long vmProjectId = vm.projectId;

        privilegeService.requireAnyPrivilegeToProjectId(user, vmProjectId);

        long actionId = actionService.createAction(vm.vmId, type, new JSONObject().toJSONString(), user.getId());

        CommandState command = null;
        switch (type) {
        case START_VM:

            Vps4StartVm.Request startRequest = new Vps4StartVm.Request();
            startRequest.actionId = actionId;
            startRequest.hfsVmId = vm.hfsVmId;

            command = Commands.execute(commandService, "Vps4StartVm", startRequest);
            break;

        case STOP_VM:
            Vps4StopVm.Request stopRequest = new Vps4StopVm.Request();
            stopRequest.actionId = actionId;
            stopRequest.hfsVmId = vm.hfsVmId;

            command = Commands.execute(commandService, "Vps4StopVm", stopRequest);
            break;

        case RESTART_VM:
            Vps4RestartVm.Request restartRequest = new Vps4RestartVm.Request();
            restartRequest.actionId = actionId;
            restartRequest.hfsVmId = vm.hfsVmId;

            command = Commands.execute(commandService, "Vps4RestartVm", restartRequest);
            break;

        default:
            throw new IllegalArgumentException("Unknown type: " + type);
        }
        logger.info("managing vm {} with command {}", type, command.commandId);

        actionService.tagWithCommand(actionId, command.commandId);

        return actionService.getAction(actionId);
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
    @Path("/provisions/")
    public Action provisionVm(ProvisionVmRequest provisionRequest) throws InterruptedException {

        logger.info("provisioning vm with orionGuid {}", provisionRequest.orionGuid);

        VirtualMachineCredit vmCredit = getVmCreditToProvision(provisionRequest.orionGuid);

        if (!(user.getShopperId().equals(vmCredit.shopperId))) {
            throw new AuthorizationException(
                    user.getShopperId() + " does not have privilege for vm request with orion guid " + vmCredit.orionGuid);
        }

        Project project = createProject(provisionRequest.orionGuid, provisionRequest.dataCenterId);

        VirtualMachineSpec spec = getVirtualMachineSpec(vmCredit);

        Image image = getImage(provisionRequest.image);
        // TODO - verify that the image matches the request (control panel, managed level, OS)

        UUID vmId = virtualMachineService.provisionVirtualMachine(vmCredit.orionGuid, provisionRequest.name,
                project.getProjectId(), spec.specId, vmCredit.managedLevel, image.imageId);

        CreateVMWithFlavorRequest hfsRequest = createHfsProvisionVmRequest(provisionRequest.image, provisionRequest.username,
                provisionRequest.password, project, spec);

        long actionId = actionService.createAction(vmId, ActionType.CREATE_VM, new JSONObject().toJSONString(), user.getId());
        logger.info("Action id: {}", actionId);

        ProvisionVmInfo vmInfo = new ProvisionVmInfo(vmId, vmCredit.managedLevel, image, project.getVhfsSgid());
        logger.info("vmInfo: {}", vmInfo.toString());

        ProvisionVm.Request request = createProvisionVmRequest(hfsRequest, actionId, vmInfo);

        CommandState command = Commands.execute(commandService, "ProvisionVm", request);
        logger.info("provisioning VM in {}", command.commandId);

        actionService.tagWithCommand(actionId, command.commandId);

        return actionService.getAction(actionId);
    }

    private ProvisionVm.Request createProvisionVmRequest(CreateVMWithFlavorRequest hfsRequest,
                                                         long actionId,
                                                         ProvisionVmInfo vmInfo) {
        ProvisionVm.Request request = new ProvisionVm.Request();
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

    private VirtualMachineCredit getVmCreditToProvision(UUID orionGuid) {
        logger.debug("Got request to provision server with guid {}", orionGuid);
        VirtualMachineCredit credit = virtualMachineService.getVirtualMachineCredit(orionGuid);
        if (credit == null) {
            throw new Vps4Exception("CREDIT_NOT_FOUND",
                    String.format("The virtual machine credit for orion guid {} was not found", orionGuid));
        }
        return credit;
    }

    private Project createProject(UUID orionGuid, int dataCenterId) {
        Project project = projectService.createProject(orionGuid.toString(), user.getId(), dataCenterId, sgidPrefix);
        if (project == null) {
            throw new Vps4Exception("PROJECT_FAILED_TO_CREATE",
                    "Failed to create new project for orionGuid " + orionGuid.toString());
        }
        return project;
    }

    private VirtualMachineSpec getVirtualMachineSpec(VirtualMachineCredit credit) {
        VirtualMachineSpec spec = virtualMachineService.getSpec(credit.tier);
        if (spec == null) {
            throw new Vps4Exception("INVALID_SPEC",
                    String.format("spec with tier %d not found", credit.tier));
        }
        return spec;
    }

    private Image getImage(String name) {
        Image image = imageService.getImage(name);
        if (image == null) {
            throw new Vps4Exception("INVALID_IMAGE",
                    String.format("image %s not found", image));
        }
        return image;
    }

    @DELETE
    @Path("/{vmId}")
    public Action destroyVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);
        if (virtualMachine == null) {
            throw new NotFoundException("vmId " + vmId + " not found");
        }

        privilegeService.requireAnyPrivilegeToProjectId(user, virtualMachine.projectId);

        // TODO verify VM status is destroyable

        long actionId = actionService.createAction(virtualMachine.vmId, ActionType.DESTROY_VM, new JSONObject().toJSONString(), user.getId());


        Vps4DestroyVm.Request request = new Vps4DestroyVm.Request();
        request.actionId = actionId;
        request.hfsVmId = virtualMachine.hfsVmId;

        CommandState command = Commands.execute(commandService, "Vps4DestroyVm", request);

        // TODO actionService.tagWithCommand(actionId, command.commandId);

        return actionService.getAction(actionId);
    }

    @GET
    @Path("/")
    public List<VirtualMachine> getVirtualMachines() {
        List<VirtualMachine> vms = virtualMachineService.getVirtualMachinesForUser(user.getId());

        vms = vms.stream().filter(vm -> vm.validUntil.isAfter(Instant.now())).collect(Collectors.toList());

        return vms;
    }



    @GET
    @Path("/{vmId}/details")
    public VirtualMachineDetails getVirtualMachineDetails(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = getVm(vmId);
        privilegeService.requireAnyPrivilegeToProjectId(user, virtualMachine.projectId);

        Vm vm = getVmFromVmVertical(virtualMachine.hfsVmId);
        return new VirtualMachineDetails(vm);
    }

    private Vm getVmFromVmVertical(long vmId) {
        try {
            Vm vm = vmService.getVm(vmId);
            return vm;
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
        privilegeService.requireAnyPrivilegeToProjectId(user, virtualMachine.projectId);

        Vm vm = getVmFromVmVertical(virtualMachine.hfsVmId);
        return new VirtualMachineWithDetails(virtualMachine, new VirtualMachineDetails(vm));
    }
}
