package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.vm.VirtualMachineService.*;
import static com.godaddy.vps4.web.vm.VmImportResource.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.security.GDUser;

public class VmImportResourceTest {
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    CreditService creditService = mock(CreditService.class);
    ProjectService projectService = mock(ProjectService.class);
    Vps4UserService vps4UserService = mock(Vps4UserService.class);
    ImageService imageService = mock(ImageService.class);
    NetworkService networkService = mock(NetworkService.class);
    ActionService actionService = mock(ActionService.class);

    VirtualMachineCredit credit;
    VmImportResource vmImportResource;
    private GDUser user;

    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();

        vmImportResource = new VmImportResource(virtualMachineService, creditService, projectService, vps4UserService, imageService, networkService, actionService);
    }

    private VirtualMachineCredit createVmCredit(UUID orionGuid, AccountStatus accountStatus, String controlPanel,
                                                int monitoring, int managedLevel, int tier, String os, Instant provisionDate) {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put("tier", String.valueOf(tier));
        planFeatures.put("managed_level", String.valueOf(managedLevel));
        planFeatures.put("control_panel_type", String.valueOf(controlPanel));
        planFeatures.put("monitoring", String.valueOf(monitoring));
        planFeatures.put("operatingsystem", os);

        Map<String, String> productMeta = new HashMap<>();
        if (provisionDate != null) {
            productMeta.put("provision_date", provisionDate.toString());
        }

        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(accountStatus)
                .withShopperID(user.getShopperId())
                .withProductMeta(productMeta)
                .withPlanFeatures(planFeatures)
                .build();
        return credit;
    }

    @Test
    public void ImportVmTest(){
        credit = createVmCredit(UUID.randomUUID(), AccountStatus.ACTIVE, "myh", 0, 0, 10, "Linux", Instant.now());
        when(creditService.getVirtualMachineCredit(credit.getOrionGuid())).thenReturn(credit);

        ImportVmRequest importVmRequest = new ImportVmRequest();
        importVmRequest.entitlementId = credit.getOrionGuid();
        importVmRequest.shopperId = user.getShopperId();
        importVmRequest.ip = "192.168.0.1";
        importVmRequest.additionalIps.add("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        importVmRequest.additionalIps.add("192.168.0.2");
        importVmRequest.additionalIps.add("192.168.0.2");

        ServerSpec spec = new ServerSpec();
        when(virtualMachineService.getSpec(credit.getTier(), ServerType.Platform.OPTIMIZED_HOSTING.getplatformId())).thenReturn(spec);

        int imageId = 1;
        when(imageService.getImageId(importVmRequest.image)).thenReturn(imageId);

        Vps4User vps4User = new Vps4User(123, importVmRequest.shopperId);
        when(vps4UserService.getOrCreateUserForShopper(importVmRequest.shopperId, credit.getResellerId())).thenReturn(vps4User);

        Project project = new Project(1, "testProject", "testSgid", Instant.now(), Instant.MAX);
        when(projectService.createProject(importVmRequest.entitlementId.toString(), vps4User.getId(), importVmRequest.sgid)).thenReturn(project);

        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.vmId = UUID.randomUUID();
        ArgumentCaptor<ImportVirtualMachineParameters> argument = ArgumentCaptor.forClass(ImportVirtualMachineParameters.class);
        when(virtualMachineService.importVirtualMachine(anyObject())).thenReturn(virtualMachine);


        VmAction action = vmImportResource.importVm(importVmRequest);


        assertEquals(ActionType.IMPORT_VM, action.type);
        assertEquals(ActionStatus.COMPLETE, action.status);
        assertEquals(virtualMachine.vmId, action.virtualMachineId);

        verify(virtualMachineService, times(1)).importVirtualMachine(argument.capture());
        ImportVirtualMachineParameters parameters = argument.getValue();
        assertEquals(importVmRequest.hfsVmId, parameters.hfsVmId);
        assertEquals(importVmRequest.entitlementId, parameters.orionGuid);
        assertEquals(spec.name, parameters.name);
        assertEquals(project.getProjectId(), parameters.projectId);
        assertEquals(spec.specId, parameters.specId);
        assertEquals(imageId, parameters.imageId);

        verify(networkService, times(1)).createIpAddress(0, action.virtualMachineId, importVmRequest.ip, IpAddress.IpAddressType.PRIMARY);
        verify(networkService, times(1)).createIpAddress(0, action.virtualMachineId, "2001:0db8:85a3:0000:0000:8a2e:0370:7334", IpAddress.IpAddressType.SECONDARY);
        verify(networkService, times(2)).createIpAddress(0, action.virtualMachineId, "192.168.0.2", IpAddress.IpAddressType.SECONDARY);
    }
}
