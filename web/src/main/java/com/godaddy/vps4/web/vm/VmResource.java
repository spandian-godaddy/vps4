package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.web.Action;
import com.godaddy.vps4.web.Vps4Api;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import gdg.hfs.vhfs.vm.*;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Vps4Api
@Api(tags = { "vms" })

@Path("/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmResource {

    private static final Logger logger = LoggerFactory.getLogger(VmResource.class);

    static final Map<Long, Action> actions = new ConcurrentHashMap<>();
    static final Map<UUID, CreateVmAction> provisionActions = new ConcurrentHashMap<>();

    static final AtomicLong actionIdPool = new AtomicLong();
    static final ExecutorService threadPool = Executors.newCachedThreadPool();

    final Vps4User user;
    final VirtualMachineService virtualMachineService;
    final PrivilegeService privilegeService;
    final ControlPanelService controlPanelService;
    final VmService vmService;
    final NetworkService hfsNetworkService;
    final OsTypeService osTypeService;
    final ProjectService projectService;
    final ImageService imageService;
    final SysAdminService sysAdminService;
    final VmUserService userService;
    final com.godaddy.vps4.network.NetworkService vps4NetworkService;
    final CPanelService cPanelService;
    final ActionService actionService;

    // TODO: Break this up into multiple classes to reduce number of
    // dependencies.
    @Inject
    public VmResource(VmUserService userService, SysAdminService sysAdminService, PrivilegeService privilegeService,
                      Vps4User user, VmService vmService, NetworkService hfsNetworkService,
                      VirtualMachineService virtualMachineService, ControlPanelService controlPanelService,
                      OsTypeService osTypeService, ProjectService projectService, ImageService imageService,
                      com.godaddy.vps4.network.NetworkService vps4NetworkService, CPanelService cPanelService,
                      ActionService actionService) {
        this.userService = userService;
        this.sysAdminService = sysAdminService;
        this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.privilegeService = privilegeService;
        this.vmService = vmService;
        this.hfsNetworkService = hfsNetworkService;
        this.controlPanelService = controlPanelService;
        this.osTypeService = osTypeService;
        this.projectService = projectService;
        this.imageService = imageService;
        this.vps4NetworkService = vps4NetworkService;
        this.cPanelService = cPanelService;
        this.actionService = actionService;
    }

    @GET
    @Path("actions/{actionId}")
    public Action getAction(@PathParam("actionId") long actionId) {
        Action action = actions.get(actionId);
        if (action == null) {
            throw new NotFoundException("actionId " + actionId + " not found");
        }
        return action;
    }

    @GET
    @Path("actions/provision/{orionGuid}")
    public CreateVmAction getProvisionActions(@PathParam("orionGuid") UUID orionGuid) {
        CreateVmAction action = provisionActions.get(orionGuid);
        if (action == null) {
            throw new NotFoundException("action or orionGuid " + orionGuid + " not found");
        }

        return action;
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
    public ManageVmAction startVm(@PathParam("vmId") long vmId) {
        ManageVmAction action = new ManageVmAction(vmId, ActionType.START_VM);
        manageVm(vmId, action);
        return action;
    }

    @POST
    @Path("{vmId}/stop")
    public ManageVmAction stopVm(@PathParam("vmId") long vmId) {
        ManageVmAction action = new ManageVmAction(vmId, ActionType.STOP_VM);
        manageVm(vmId, action);
        return action;
    }

    @POST
    @Path("{vmId}/restart")
    public ManageVmAction restartVm(@PathParam("vmId") long vmId) {
        ManageVmAction action = new ManageVmAction(vmId, ActionType.RESTART_VM);
        manageVm(vmId, action);
        return action;
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
     * Check if user is allowed to access the vm
     * @param vmProjectId
     * @return true if user has access, false otherwise
     */
    private boolean userExistsOnVm(long vmProjectId) {
        try {
            privilegeService.requireAnyPrivilegeToSgid(user, vmProjectId);
            return true;
        } catch (Exception ex) {
            logger.error("Could not ascertain user privileges. ", ex);
            return false;
        }
    }

    /**
     * Manage the vm to perform actions like start / stop / restart vm.
     * @param vmId
     * @param action
     */
    private void manageVm (long vmId, ManageVmAction action) {

        action.status = ActionStatus.IN_PROGRESS;
        long actionId = actionService.createAction(vmId, action.getActionType(), "{}", user.getId());
        action.setActionId(actionId);

        // Check if vm exists and user has access to the vm.
        long vmProjectId;
        try {
            vmProjectId = getVmProjectId(vmId);
        } catch (Exception ex) {
            String message = String.format("Could not get project id for requested vm id: %d.", vmId);
            logger.error("Could not get project id for VM vm id: {} ", vmId, ex);
            action.status = ActionStatus.ERROR;
            action.setMessage(message);
            actionService.failAction(vmId, "{}", message);
            return;
        }

        if (!userExistsOnVm(vmProjectId)) {
            String message = String.format("User %s does not exist on vm id: %d%n ", user, vmId);
            logger.error(message);
            action.status = ActionStatus.ERROR;
            action.setMessage(message);
            actionService.failAction(vmId, "{}", message);
            return;
        }


        ManageVmWorker worker = new ManageVmWorker(vmService, actionService, vmId, action);
        threadPool.execute(() -> {
            try {
                worker.run();
            } catch (Vps4Exception e) {
                action.status = ActionStatus.ERROR;
                action.setMessage(e.getMessage());
                actionService.failAction(vmId, "{}", e.getMessage());
            }
        });
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
    public CreateVmAction getProvisionAction(@PathParam("orionGuid") UUID orionGuid) {
        return provisionActions.get(orionGuid);
    }

    public static class ProvisionVmInfo {
        public UUID orionGuid;
        public String name;
        public long projectId;
        public int specId;
        public int managedLevel;
        public Image image;

        public ProvisionVmInfo(UUID orionGuid, String name, long projectId, int specId,
                int managedLevel, Image image) {
            this.orionGuid = orionGuid;
            this.name = name;
            this.projectId = projectId;
            this.specId = specId;
            this.managedLevel = managedLevel;
            this.image = image;
        }
    }

    @POST
    @Path("/provisions/")
    public CreateVmAction provisionVm(ProvisionVmRequest provisionRequest) throws InterruptedException {

        logger.info("provisioning vm with orionGuid {}", provisionRequest.orionGuid);

        VirtualMachineRequest request = getVmRequestToProvision(provisionRequest.orionGuid);

        Project project = createProject(provisionRequest.orionGuid, provisionRequest.dataCenterId);

        VirtualMachineSpec spec = getVirtualMachineSpec(request);

        Image image = getImage(provisionRequest.image);
        // TODO - verify that the image matches the request (control panel, managed level, OS)

        // FIXME need to get the action back to the caller so they can poll the status/steps/ticks
        CreateVMRequest hfsRequest = createHfsProvisionVmRequest(provisionRequest.image, provisionRequest.username,
                provisionRequest.password, project, spec);
        CreateVmAction action = new CreateVmAction(hfsRequest);
        action.actionId = actionIdPool.incrementAndGet();
        logger.debug("Action.actionid = {}", action.actionId);
        action.project = project;
        actions.put(action.actionId, action);
        provisionActions.put(provisionRequest.orionGuid, action);
        ProvisionVmInfo vmInfo = new ProvisionVmInfo(provisionRequest.orionGuid, provisionRequest.name, project.getProjectId(),
                spec.specId, request.managedLevel, image);
        final ProvisionVmWorker provisionWorker = new ProvisionVmWorker(vmService, hfsNetworkService, sysAdminService, userService,
                vps4NetworkService, virtualMachineService, cPanelService,
                action, threadPool, vmInfo);
        threadPool.execute(() -> {
            provisionWorker.run();
        });

        if (action.status != ActionStatus.ERROR) {
            action.step = CreateVmStep.SetupComplete;
            action.status = ActionStatus.COMPLETE;
        }

        return action;
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

        DestroyVmAction action = new DestroyVmAction();
        action.actionId = actionIdPool.incrementAndGet();
        action.status = ActionStatus.IN_PROGRESS;
        action.virtualMachine = virtualMachine;

        actions.put(action.actionId, action);

        threadPool
                .execute(new DestroyVmWorker(action, vmService, hfsNetworkService, vps4NetworkService, virtualMachineService, threadPool));

        return action;
    }

    public static class CreateVmAction extends Action {

        public CreateVmAction(CreateVMRequest request) {
            hfsProvisionRequest = request;
        }

        public final CreateVMRequest hfsProvisionRequest;

        public volatile Project project;
        public volatile Vm vm;
        public volatile IpAddress ip;
        public volatile CreateVmStep step;

        public CreateVmStep[] steps = CreateVmStep.values();

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CreateVMAction [id: ");
            stringBuilder.append(actionId);
            stringBuilder.append(", status: ");
            stringBuilder.append(status);
            stringBuilder.append(", step: ");
            stringBuilder.append(step);
            stringBuilder.append(", message: ");
            stringBuilder.append(message);
            if (project != null) {
                stringBuilder.append(", project: ");
                stringBuilder.append(project.getName());
            }
            if (vm != null) {
                stringBuilder.append(", vmId: ");
                stringBuilder.append(vm.vmId);
            }
            if (ip != null) {
                stringBuilder.append(", addressId: ");
                stringBuilder.append(ip.addressId);
            }
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public static class DestroyVmAction extends Action {
        public VirtualMachine virtualMachine;
    }

}
