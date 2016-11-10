package com.godaddy.vps4.web.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.User;
import com.godaddy.vps4.vm.CombinedVm;
import com.godaddy.vps4.vm.ControlPanelService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.OsTypeService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineRequest;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineSpec;
import com.godaddy.vps4.web.Action;
import com.godaddy.vps4.web.Action.ActionStatus;
import com.godaddy.vps4.web.Vps4Api;

import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import gdg.hfs.vhfs.vm.CreateVMRequest;
import gdg.hfs.vhfs.vm.Flavor;
import gdg.hfs.vhfs.vm.FlavorList;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;
import io.swagger.annotations.Api;

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

    final User user;
    final VirtualMachineService virtualMachineService;
    final PrivilegeService privilegeService;
    final ControlPanelService controlPanelService;
    final VmService vmService;
    final NetworkService hfsNetworkService;
    final OsTypeService osTypeService;
    final ProjectService projectService;
    final ImageService imageService;
    final SysAdminService sysAdminService;
    final com.godaddy.vps4.network.NetworkService vps4NetworkService;
    final CPanelService cPanelService;

    // TODO: Break this up into multiple classes to reduce number of
    // dependencies.
    @Inject
    public VmResource(SysAdminService sysAdminService, PrivilegeService privilegeService, User user, VmService vmService,
            NetworkService hfsNetworkService, VirtualMachineService virtualMachineService, ControlPanelService controlPanelService,
            OsTypeService osTypeService, ProjectService projectService, ImageService imageService,
            com.godaddy.vps4.network.NetworkService vps4NetworkService, CPanelService cPanelService) {
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
        // TODO - verify that the image maches the request (control panel, managed level, OS)

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
        final ProvisionVmWorker provisionWorker = new ProvisionVmWorker(vmService, hfsNetworkService, sysAdminService,
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
