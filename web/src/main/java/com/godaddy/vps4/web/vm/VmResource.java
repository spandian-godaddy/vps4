package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.web.Vps4Api;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.vm.*;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Vps4Api
@Api(tags = { "vms" })

@Path("/vms")
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

    @Inject
    public VmResource(PrivilegeService privilegeService,
                      Vps4User user, VmService vmService,
                      VirtualMachineService virtualMachineService,
                      ProjectService projectService, ImageService imageService,
                      com.godaddy.vps4.network.NetworkService vps4NetworkService,
                      CPanelService cPanelService,
                      ActionService actionService) {
        this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.privilegeService = privilegeService;
        this.vmService = vmService;
        this.projectService = projectService;
        this.imageService = imageService;
        this.actionService = actionService;
    }

    @GET
    @Path("actions/{actionId}")
    public Action getAction(@PathParam("actionId") long actionId) {
        Action action = actionService.getAction(actionId);
        if (action == null) {
            throw new NotFoundException("actionId " + actionId + " not found");
        }
        return action;
    }

    @GET
    @Path("actions/provision/{orionGuid}")
    public Action getProvisionActions(@PathParam("orionGuid") UUID orionGuid) {
        //CreateVmAction action = provisionActions.get(orionGuid);

        Action action = getActionFromOrionGuid(orionGuid);
        if (action == null) {
            throw new NotFoundException("action or orionGuid " + orionGuid + " not found");
        }

        return action;
    }

    protected Action getActionFromOrionGuid(UUID orionGuid) {

        // TODO
        // - add an action_id to the provision request table
        // - update that column when a provision is started for a specific provision request
        // - add a way to look that up through actionService (or something)
        return null;
    }

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
    @Path("/{orionGuid}")
    public CombinedVm getVm(@PathParam("orionGuid") UUID orionGuid) {

        logger.info("getting vm with id {}", orionGuid);

        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(orionGuid);

        if (virtualMachine == null) {
            // TODO need to return 404 here
            throw new IllegalArgumentException("Unknown VM ID: " + orionGuid);
        }

        privilegeService.requireAnyPrivilegeToSgid(user, virtualMachine.projectId);

        VirtualMachineRequest req = virtualMachineService.getVirtualMachineRequest(orionGuid);

        // now reach out to the VM vertical to get all the details
        Vm vm = getVmFromVmVertical(virtualMachine.vmId);

        return new CombinedVm(vm, virtualMachine, req);

    }

    private Vm getVmFromVmVertical(long vmId) {
        Vm vm = vmService.getVm(vmId);
        if (vm == null) {
            throw new IllegalArgumentException("Cannot find VM ID " + vmId + " in vm vertical");
        }
        return vm;
    }

    @GET
    @Path("/requests/{orionGuid}")
    public VirtualMachineRequest getVmRequest(@PathParam("orionGuid") UUID orionGuid) {
        logger.info("getting vm request with orionGuid {}", orionGuid);
        return virtualMachineService.getVirtualMachineRequest(orionGuid);
    }

    @POST
    @Path("/")
    public VirtualMachineRequest createVm(@QueryParam("orionGuid") UUID orionGuid, @QueryParam("operatingSystem") String operatingSystem,
            @QueryParam("tier") int tier, @QueryParam("controlPanel") String controlPanel, @QueryParam("managedLevel") int managedLevel) {

        logger.info("creating new vm request for orionGuid {}", orionGuid);
        virtualMachineService.createVirtualMachineRequest(orionGuid, operatingSystem, controlPanel, tier, managedLevel);
        return virtualMachineService.getVirtualMachineRequest(orionGuid);

    }

    @POST
    @Path("{vmId}/start")
    public Action startVm(@PathParam("vmId") long vmId) throws VmNotFoundException {
        return manageVm(vmId, ActionType.START_VM);
    }

    @POST
    @Path("{vmId}/stop")
    public Action stopVm(@PathParam("vmId") long vmId) throws VmNotFoundException {
        return manageVm(vmId, ActionType.STOP_VM);
    }

    @POST
    @Path("{vmId}/restart")
    public Action restartVm(@PathParam("vmId") long vmId) throws VmNotFoundException {
        return manageVm(vmId, ActionType.RESTART_VM);
    }

    /**
     * Check if vm exists
     * @param vmId
     * @return virtualmachine project id if found
     * @throws VmNotFoundException
     */
    private long getVmProjectId(long vmId) throws VmNotFoundException {
        VirtualMachine vm;

        try {
            vm = virtualMachineService.getVirtualMachine(vmId);
            if(vm == null) {
                throw new VmNotFoundException("Could not find VM for specified vm id: " + vmId );
            }
        } catch (Exception ex) {
            logger.error("Could not find vm with VM ID: {} ", vmId, ex);
            throw new VmNotFoundException( "Could not find vm with VM ID: " + vmId, ex);
        }
        return vm.projectId;
    }

    /**
     * Manage the vm to perform actions like start / stop / restart vm.
     * @param vmId
     * @param action
     */
    private Action manageVm (long vmId, ActionType type) throws VmNotFoundException {

        // Check if vm exists and user has access to the vm.
        long vmProjectId = getVmProjectId(vmId);

        privilegeService.requireAnyPrivilegeToSgid(user, vmProjectId);

        long actionId = actionService.createAction(vmId, type, "", user.getId());

        // FIXME orchestration-client call
        switch(type) {
        case START_VM:
        case STOP_VM:
        case RESTART_VM:
        }

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

    @GET
    @Path("/provisions/{orionGuid}")
    public Action getProvisionAction(@PathParam("orionGuid") UUID orionGuid) {

        return getActionFromOrionGuid(orionGuid);
    }

    @POST
    @Path("/provisions/")
    public Action provisionVm(ProvisionVmRequest provisionRequest) throws InterruptedException {

        logger.info("provisioning vm with orionGuid {}", provisionRequest.orionGuid);

        VirtualMachineRequest request = getVmRequestToProvision(provisionRequest.orionGuid);

        Project project = createProject(provisionRequest.orionGuid, provisionRequest.dataCenterId);

        VirtualMachineSpec spec = getVirtualMachineSpec(request);

        Image image = getImage(provisionRequest.image);
        // TODO - verify that the image matches the request (control panel, managed level, OS)

        // FIXME need to get the action back to the caller so they can poll the status/steps/ticks
        CreateVMRequest hfsRequest = createHfsProvisionVmRequest(provisionRequest.image, provisionRequest.username,
                provisionRequest.password, project, spec);

        // FIXME we don't have the vmId here yet, since we're using the HFS vmId and we haven't made the HFS
        //       VM request yet
        long vmId = 0; // ?
        long actionId = actionService.createAction(vmId, ActionType.CREATE_VM, "", user.getId());

        //action.project = project;
        //actions.put(action.actionId, action);
        //provisionActions.put(provisionRequest.orionGuid, action);

        ProvisionVmInfo vmInfo = new ProvisionVmInfo(provisionRequest.orionGuid, provisionRequest.name, project.getProjectId(),
                spec.specId, request.managedLevel, image);

        // FIXME orchestration client
        //final ProvisionVmWorker provisionWorker = new ProvisionVmWorker(vmService, hfsNetworkService, sysAdminService, userService,
        //        vps4NetworkService, virtualMachineService, cPanelService,
        //        action, threadPool, vmInfo);
        //threadPool.execute(() -> {
        //    provisionWorker.run();
        //});

//        if (action.status != ActionStatus.ERROR) {
//            action.step = CreateVmStep.SetupComplete;
//            action.status = ActionStatus.COMPLETE;
//        }

        return actionService.getAction(actionId);
    }

    private CreateVMRequest createHfsProvisionVmRequest(String image, String username, String password, Project project,
            VirtualMachineSpec spec) {
        CreateVMRequest hfsProvisionRequest = new CreateVMRequest();
        hfsProvisionRequest.cpuCores = (int) spec.cpuCoreCount;
        hfsProvisionRequest.diskGiB = (int) spec.diskGib;
        hfsProvisionRequest.ramMiB = (int) spec.memoryMib;

        hfsProvisionRequest.sgid = project.getVhfsSgid();
        hfsProvisionRequest.image_name = image;

        hfsProvisionRequest.username = username;
        hfsProvisionRequest.password = password;
        return hfsProvisionRequest;
    }

    private VirtualMachineRequest getVmRequestToProvision(UUID orionGuid) {
        VirtualMachineRequest request = virtualMachineService.getVirtualMachineRequest(orionGuid);
        if (request == null) {
            throw new Vps4Exception("REQUEST_NOT_FOUND",
                    String.format("The virtual machine request for orion guid {} was not found", orionGuid));
        }
        return request;
    }

    private Project createProject(UUID orionGuid, int dataCenterId) {
        Project project = projectService.createProject(orionGuid.toString(), user.getId(), dataCenterId);
        if (project == null) {
            throw new Vps4Exception("PROJECT_FAILED_TO_CREATE", "Failed to create new project for orionGuid " + orionGuid.toString());
        }
        return project;
    }

    private VirtualMachineSpec getVirtualMachineSpec(VirtualMachineRequest request) {
        VirtualMachineSpec spec = virtualMachineService.getSpec(request.tier);
        if (spec == null) {
            throw new Vps4Exception("INVALID_SPEC", String.format("spec with tier %d not found", request.tier));
        }
        return spec;
    }

    private Image getImage(String name) {
        Image image = imageService.getImage(name);
        if (image == null) {
            throw new Vps4Exception("INVALID_IMAGE", String.format("image %s not found", image));
        }
        return image;
    }

    @DELETE
    @Path("vms/{vmId}")
    public Action destroyVm(@PathParam("vmId") long vmId) {
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);
        if (virtualMachine == null) {
            throw new NotFoundException("vmId " + vmId + " not found");
        }

        // TODO verify VM status is destroyable

        // FIXME generic Action
        long actionId = actionService.createAction(vmId, ActionType.CREATE_VM, "", user.getId());
//        DestroyVmAction action = new DestroyVmAction();
//        action.actionId = actionIdPool.incrementAndGet();
//        action.status = ActionStatus.IN_PROGRESS;
//        action.virtualMachine = virtualMachine;
//
//        actions.put(action.actionId, action);
//
          // FIXME orchestration client
//        threadPool
//                .execute(new DestroyVmWorker(action, vmService, hfsNetworkService, vps4NetworkService, virtualMachineService, threadPool));

        return actionService.getAction(actionId);
    }

}
