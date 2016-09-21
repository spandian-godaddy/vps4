package com.godaddy.vps4.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.sql.DataSource;
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

import com.godaddy.vps4.hfs.CreateVMRequest;
import com.godaddy.vps4.hfs.Flavor;
import com.godaddy.vps4.hfs.Vm;
import com.godaddy.vps4.hfs.VmService;
import com.godaddy.vps4.hfs.VmService.FlavorList;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.User;
import com.godaddy.vps4.vm.CombinedVm;
import com.godaddy.vps4.vm.ControlPanelService;
import com.godaddy.vps4.vm.HostnameGenerator;
import com.godaddy.vps4.vm.OsTypeService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineSpec;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmsResource {

	private static final Logger logger = LoggerFactory.getLogger(VmsResource.class);

	final User user;
	final VirtualMachineService virtualMachineService;
	final PrivilegeService privilegeService;
	final ControlPanelService controlPanelService;
    final VmService vmService;
	final OsTypeService osTypeService;
	final ProjectService projectService;
	final Map<Long, VmAction> actions = new ConcurrentHashMap<>();
	final AtomicLong actionIdPool = new AtomicLong();
	final ExecutorService threadPool = Executors.newCachedThreadPool();

	//TODO: Break this up into multiple classes to reduce number of dependencies.
	@Inject
	public VmsResource(DataSource dataSource, 
            PrivilegeService privilegeService,
			User user,
            VmService vmService,
            VirtualMachineService virtualMachineService,
            ControlPanelService controlPanelService,
            OsTypeService osTypeService,
            ProjectService projectService
            ) {
		this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.privilegeService = privilegeService;
        this.vmService = vmService;
        this.controlPanelService = controlPanelService;
        this.osTypeService = osTypeService;
        this.projectService = projectService;
	}
	
	@GET
	@Path("actions/{actionId}")
	public VmAction getAction(@PathParam("actionId") long actionId) {
		
		VmAction action = actions.get(actionId);
		if (action == null) {
			throw new NotFoundException("actionId " + actionId + " not found");
		}
		
		return action;
	}
	
	@GET
	@Path("/flavors")
	public List<Flavor> getFlavors() {
		
		logger.info("getting flavors from HFS...");
		
		FlavorList flavorList = vmService.getFlavors();
		logger.info("flavorList: {}", flavorList);
		if (flavorList != null && flavorList.results != null) {
			return flavorList.results;
		}
		return new ArrayList<>();
	}
	
	@GET
	@Path("/{vmId}")
	public CombinedVm getVm(@PathParam("vmId") int vmId) {

		logger.info("getting vm with id {}", vmId);
//		return new CombinedVm();

		// first check our database to see if we have a record of this VM
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);
        if (virtualMachine == null) {
            // TODO need to return 404 here
            throw new IllegalArgumentException("Unknown VM ID: " + vmId);
        }

        privilegeService.requireAnyPrivilegeToSgid(user, virtualMachine.projectId);

        // now reach out to the VM vertical to get all the details
        Vm vm = vmService.getVm(vmId);
        if (vm == null) {
            throw new IllegalArgumentException("Cannot find VM ID " + vmId + " in vertical");
        }

        return new CombinedVm(vm, virtualMachine);

	}

	@POST
	@Path("/")
	public boolean createVm(@QueryParam("orionGuid") UUID orionGuid,
							@QueryParam("osType") String osType,
							@QueryParam("tier") int tier,
							@QueryParam("controlPanel") String controlPanel,
							@QueryParam("managedLevel") int managedLevel) {

		int controlPanelId = controlPanelService.getControlPanelId(controlPanel);
		int osTypeId = osTypeService.getOsTypeId(osType);
		VirtualMachineSpec spec = virtualMachineService.getSpec(tier);
		
		Project project = projectService.createProject(orionGuid.toString(), user.getId());
		virtualMachineService.createVirtualMachine(orionGuid, project.getProjectId(), osTypeId, controlPanelId, spec.specId, managedLevel);
		
		return true;
	}

	@POST
	@Path("/{vmId}/provision")
	public CombinedVm provisionVm(@QueryParam("name") String name,
							      @QueryParam("orionGuid") UUID orionGuid,
							      @QueryParam("image") String image,
							      @QueryParam("dataCenter") int dataCenterId,
							      @QueryParam("username") String username,
							      @QueryParam("password") String password) throws InterruptedException {
		
		VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(orionGuid);
		Project project = projectService.getProject(virtualMachine.projectId);

		logger.info("creating new vm");
		
		CreateVmAction action = new CreateVmAction();
		action.type = ActionType.CREATE;
		action.actionId = actionIdPool.incrementAndGet();
		action.status = ActionStatus.IN_PROGRESS;
		
        CreateVMRequest hfsCreateRequest = new CreateVMRequest();
        hfsCreateRequest.cpuCores = (int) virtualMachine.spec.cpuCoreCount;
        hfsCreateRequest.diskGiB = (int) virtualMachine.spec.diskGib;
        hfsCreateRequest.ramMiB = (int) virtualMachine.spec.memoryMib;

        hfsCreateRequest.sgid = project.getVhfsSgid();
        hfsCreateRequest.image_name = image;
        hfsCreateRequest.os = image;

        // TODO: This will need to be replaced with a generated hostname
        hfsCreateRequest.hostname = HostnameGenerator.GetHostname();

        hfsCreateRequest.username = username;
        hfsCreateRequest.password = password;

        action.hfsCreateRequest = hfsCreateRequest;

		actions.put(action.actionId, action);
		
		CreateVmWorker worker = new CreateVmWorker(vmService, action); 
		threadPool.execute(worker);
		
		//Wait for the VM Id
		synchronized (worker) {
		    worker.wait();
		}
		
		virtualMachineService.updateVirtualMachine(orionGuid, name, action.vm.vmId, image, dataCenterId);
		
		return new CombinedVm(action.vm, virtualMachine);
	}
	
	@DELETE
	@Path("vms/{vmId}")
	public VmAction destroyVm(@PathParam("vmId") long vmId) {
		
		Vm vm = vmService.getVm(vmId);
		if (vm == null) {
			throw new NotFoundException("vmId " + vmId + " not found");
		}
		
		// TODO verify VM status is destroyable
		
		DestroyVmAction action = new DestroyVmAction();
		action.type = ActionType.DESTROY;
		action.actionId = actionIdPool.incrementAndGet();
		action.status = ActionStatus.IN_PROGRESS;
		action.vmId = vmId;
		
		actions.put(action.actionId, action);
		
		threadPool.execute(new DestroyVmWorker(vmService, action));
			
		return action;
	}
	
	public enum ActionStatus {
		IN_PROGRESS,
		COMPLETE,
		ERROR
	}
	
	public enum ActionType {
		CREATE,
		DESTROY
	}
	
	public static class VmAction {
		public long actionId;
		public ActionType type;
		public volatile ActionStatus status;
		public volatile String message;
	}
	
	public static class CreateVmAction extends VmAction {
		public CreateVMRequest hfsCreateRequest = new CreateVMRequest();
		
		public volatile Vm vm;
	}
	
	public static class DestroyVmAction extends VmAction {
		public long vmId;
	}
}
