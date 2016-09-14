package com.godaddy.vps4.web;

import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.User;
import com.godaddy.vps4.vm.CombinedVm;
import com.godaddy.vps4.vm.ControlPanelService;
import com.godaddy.vps4.vm.OsTypeService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineSpec;

import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;
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

	@Inject
	public VmsResource(DataSource dataSource,
            PrivilegeService privilegeService,
			User user,
            VmService vmService,
            VirtualMachineService virtualMachineService,
            ControlPanelService controlPanelService,
            OsTypeService osTypeService) {
		this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.privilegeService = privilegeService;
        this.vmService = vmService;
        this.controlPanelService = controlPanelService;
        this.osTypeService = osTypeService;
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
		
		virtualMachineService.createVirtualMachine(orionGuid, osTypeId, controlPanelId, spec.specId, managedLevel);
		
		
		return true;

	}

	@POST
	@Path("/{vmId}/provision")
	public CombinedVm provisionVm(@QueryParam("name") String name,
   							   @QueryParam("vmId") long vmId,
							   @QueryParam("image") String image,
							   @QueryParam("dataCenter") long projectId,
							   @QueryParam("username") String username,
							   @QueryParam("password") String password) {

		logger.info("creating new vm");

		return new CombinedVm();
	}
}
