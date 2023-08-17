package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.InsertVirtualMachineParameters;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser.Role;
import com.godaddy.vps4.web.security.RequiresRole;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateCreditIsNotInUse;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {Role.ADMIN, Role.MIGRATION})
public class VmImportResource {

    private final VirtualMachineService virtualMachineService;
    private final CreditService creditService;
    private final ProjectService projectService;
    private final Vps4UserService vps4UserService;
    private final ImageService imageService;
    private final NetworkService networkService;
    private final ActionService actionService;
    private final VmActionResource vmActionResource;
    private final VmUserService vmUserService;
    private final Config config;
    private final int defaultDatacenterId;

    @Inject
    public VmImportResource(VirtualMachineService virtualMachineService,
                            CreditService creditService,
                            ProjectService projectService,
                            Vps4UserService vps4UserService,
                            ImageService imageService,
                            NetworkService networkService,
                            ActionService actionService,
                            VmActionResource vmActionResource,
                            VmUserService vmUserService,
                            Config config) {
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.projectService = projectService;
        this.vps4UserService = vps4UserService;
        this.imageService = imageService;
        this.networkService = networkService;
        this.actionService = actionService;
        this.vmActionResource = vmActionResource;
        this.config = config;
        this.vmUserService = vmUserService;
        defaultDatacenterId = Integer.parseInt(config.get("vps4.datacenter.defaultId"));
    }

    public static class ImportVmRequest {
        public String shopperId;
        public long hfsVmId;
        public UUID entitlementId;
        public String ip;
        public List<ImportVmIpAddress> additionalIps;
        public String sgid;
        public String image;
        public String name;
        public String username;

        public ImportVmRequest() {
            additionalIps = new ArrayList<>();
        }
    }

    public static class ImportVmIpAddress {
        public long hfsIpAddressId;
        public String ip;
    }

    @POST
    @Path("/importVm")
    public VmAction importVm(ImportVmRequest importVmRequest) {
        int dataCenterId = Integer.parseInt(config.get("imported.datacenter.defaultId"));
        int platformId = ServerType.Platform.OPTIMIZED_HOSTING.getplatformId();
        VirtualMachineCredit virtualMachineCredit = getAndValidateUserAccountCredit(creditService, importVmRequest.entitlementId, importVmRequest.shopperId);
        try {
            validateCreditIsNotInUse(virtualMachineCredit);
        } catch (Vps4Exception e) {
            throw new Vps4Exception("DUPLICATE", "The entitlement for this import has already been provisioned, this import is a " +
                    "duplicate", e);
        }
        ServerSpec serverSpec = virtualMachineService.getSpec(virtualMachineCredit.getTier(), platformId);

        long imageId = getOrInsertImage(importVmRequest);

        Vps4User vps4User = vps4UserService.getOrCreateUserForShopper(importVmRequest.shopperId, virtualMachineCredit.getResellerId(),
                virtualMachineCredit.getCustomerId());
        Project project = projectService.createProject(importVmRequest.entitlementId.toString(), vps4User.getId(), importVmRequest.sgid);


        String importName = importVmRequest.name == null ? importVmRequest.ip : importVmRequest.name;

        InsertVirtualMachineParameters parameters = new InsertVirtualMachineParameters(importVmRequest.hfsVmId,
                                                                                       importVmRequest.entitlementId,
                                                                                       importName,
                                                                                       project.getProjectId(),
                                                                                       serverSpec.specId,
                                                                                       imageId,
                                                                                       dataCenterId,
                                                                                       null);

        VirtualMachine virtualMachine = virtualMachineService.importVirtualMachine(parameters);

        creditService.claimVirtualMachineCredit(importVmRequest.entitlementId, defaultDatacenterId, virtualMachine.vmId);

        creditService.setCommonName(importVmRequest.entitlementId, importName);

        importIpAddresses(importVmRequest, virtualMachine);

        if (StringUtils.isNotBlank(importVmRequest.username)) {
            vmUserService.createUser(importVmRequest.username, virtualMachine.vmId);
        }

        return createReturnAction(virtualMachine);
    }

    private long getOrInsertImage(ImportVmRequest importVmRequest) {
        long imageId = imageService.getImageIdByHfsName(importVmRequest.image);
        if(imageId==0){
            imageId = imageService.insertImage(0, 1, importVmRequest.image, 3, importVmRequest.image, true);
        }
        return imageId;
    }

    private void importIpAddresses(ImportVmRequest importVmRequest, VirtualMachine virtualMachine) {
        networkService.createIpAddress(0, virtualMachine.vmId, importVmRequest.ip, IpAddress.IpAddressType.PRIMARY);
        if(importVmRequest.additionalIps != null && !importVmRequest.additionalIps.isEmpty()){
            for (ImportVmIpAddress ipAddress : importVmRequest.additionalIps) {
                networkService.createIpAddress(ipAddress.hfsIpAddressId, virtualMachine.vmId, ipAddress.ip, IpAddress.IpAddressType.SECONDARY);
           }
        }
    }

    private VmAction createReturnAction(VirtualMachine virtualMachine) {
        long actionId = actionService.createAction(virtualMachine.vmId, ActionType.IMPORT_VM, null, "EMEA Migrations");
        actionService.completeAction(actionId, null, null);

        return vmActionResource.getVmAction(virtualMachine.vmId, actionId);
    }
}
