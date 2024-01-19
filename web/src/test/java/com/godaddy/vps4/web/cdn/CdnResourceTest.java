package com.godaddy.vps4.web.cdn;

import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnSite;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.orchestration.cdn.Vps4SubmitCdnCreation;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class CdnResourceTest {
    private UUID vmId;

    private CdnResource resource;
    private VmResource vmResource = mock(VmResource.class);
    private CreditService creditService = mock(CreditService.class);
    private CdnService cdnService = mock(CdnService.class);
    private CdnDataService cdnDataService = mock(CdnDataService.class);
    private ActionService actionService = mock(ActionService.class);
    private CommandService commandService = mock(CommandService.class);
    private Cryptography cryptography = mock(Cryptography.class);
    private VirtualMachine vm;
    private VirtualMachineCredit credit;
    private DataCenter dataCenter;
    private CdnSite cdnSite;
    private CdnDetail cdnDetail;
    private GDUser userShopper;
    private GDUser userEmployee;
    private GDUser userEmployee2Shopper;
    private ResultSubset<Action> actions;

    @Before
    public void setupTest() {
        userShopper = GDUserMock.createShopper();
        userEmployee = GDUserMock.createEmployee();
        userEmployee2Shopper = GDUserMock.createEmployee2Shopper();
        vmId = UUID.randomUUID();
        long hfsVmId = 1111;
        ServerType vmServerType = new ServerType();
        vmServerType.platform = ServerType.Platform.OPTIMIZED_HOSTING;
        ServerSpec vmSpec = new ServerSpec();
        vmSpec.serverType = vmServerType;
        vmSpec.ipAddressLimit = 2;
        dataCenter = new DataCenter(1, "phx3");
        vm = new VirtualMachine(vmId,
                hfsVmId,
                UUID.randomUUID(),
                1,
                vmSpec,
                "Unit Test Vm",
                null,
                null,
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS),
                Instant.now().plus(24, ChronoUnit.HOURS),
                null,
                null,
                0,
                UUID.randomUUID(),
                dataCenter);

        credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountStatus(AccountStatus.ACTIVE)
                .withShopperID(userShopper.getShopperId())
                .build();

        cdnSite = new CdnSite();
        cdnSite.siteId = "fakeSiteId";
        cdnSite.domain = "fakeDomain.com";
        cdnSite.anyCastIP = "0.0.0.0";
        cdnSite.status = CdnStatus.SUCCESS;
        cdnSite.planId = "fakePlanId";

        cdnDetail = new CdnDetail();
        cdnDetail.siteId = "fakeSiteDetailId";

        Action vmAction = mock(Action.class);
        actions = new ResultSubset<>(Collections.emptyList(), 0);

        when(vmResource.getVm(vmId)).thenReturn(vm);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);

        when(cdnService.getCdnSites(anyString(), anyString(), any())).thenReturn(Collections.singletonList(cdnSite));
        when(cdnService.getCdnSiteDetail(anyString(), anyString(), anyString(), any())).thenReturn(cdnDetail);

        when(cdnDataService.getActiveCdnSitesOfVm(eq(vmId))).thenReturn(Arrays.asList(new VmCdnSite()));

        when(actionService.getAction(anyLong())).thenReturn(vmAction);
        when(actionService.getActionList(any())).thenReturn(actions);

        when(commandService.executeCommand(any())).thenReturn(new CommandState());
    }

    @Test
    public void testGetCdnSitesShopper() {
        resource = new CdnResource(userShopper, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        List<CdnSite> response = resource.getActiveCdnSites(vmId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(cdnService, times(1))
                .getCdnSites(credit.getShopperId(), userShopper.getToken().getJwt().getParsedString(), vm.vmId);

        assertEquals(1, response.size());
        assertSame(cdnSite, response.get(0));
    }

    @Test
    public void testGetCdnSitesE2S() {
        resource = new CdnResource(userEmployee2Shopper, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        List<CdnSite> response = resource.getActiveCdnSites(vmId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(cdnService, times(1))
                .getCdnSites(credit.getShopperId(), userShopper.getToken().getJwt().getParsedString(), vm.vmId);

        assertEquals(1, response.size());
        assertSame(cdnSite, response.get(0));
    }

    @Test
    public void testGetCdnSitesEmployee() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        List<CdnSite> response = resource.getActiveCdnSites(vmId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(cdnService, times(1))
                .getCdnSites(credit.getShopperId(), null, vm.vmId);

        assertEquals(1, response.size());
        assertSame(cdnSite, response.get(0));
    }

    @Test
    public void testGetCdnSiteDetailShopper() {
        resource = new CdnResource(userShopper, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        CdnDetail response = resource.getCdnSiteDetail(vmId, cdnDetail.siteId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(cdnService, times(1))
                .getCdnSiteDetail(credit.getShopperId(), userShopper.getToken().getJwt().getParsedString(), cdnDetail.siteId, vm.vmId);

        assertSame(cdnDetail, response);
    }

    @Test
    public void testGetCdnSiteDetailE2S() {
        resource = new CdnResource(userEmployee2Shopper, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        CdnDetail response = resource.getCdnSiteDetail(vmId, cdnDetail.siteId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(cdnService, times(1))
                .getCdnSiteDetail(credit.getShopperId(), userShopper.getToken().getJwt().getParsedString(), cdnDetail.siteId, vm.vmId);

        assertSame(cdnDetail, response);
    }

    @Test
    public void testGetCdnSiteDetailEmployee() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        CdnDetail response = resource.getCdnSiteDetail(vmId, cdnDetail.siteId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(cdnService, times(1))
                .getCdnSiteDetail(credit.getShopperId(), null, cdnDetail.siteId, vm.vmId);

        assertSame(cdnDetail, response);
    }

    @Test
    public void testDeleteCdnSiteCreatesAction() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        resource.deleteCdnSite(vmId, cdnDetail.siteId);
        verify(actionService, times(1))
                .createAction(vmId, ActionType.DELETE_CDN, "{}", userEmployee.getUsername());
    }

    @Test
    public void testDeleteCdnSiteExecutesCommand() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        resource.deleteCdnSite(vmId, cdnDetail.siteId);
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(argument.capture());
        CommandGroupSpec cmdGroup = argument.getValue();
        assertEquals("Vps4RemoveCdnSite", cmdGroup.commands.get(0).command);
    }

    @Test
    public void testDeleteCdnSiteConflictingAction() {
        Action conflictAction = mock(Action.class);
        List<ActionType> conflictTypes = Arrays.asList(ActionType.DELETE_CDN, ActionType.MODIFY_CDN, ActionType.CREATE_CDN, ActionType.VALIDATE_CDN);
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);

        for (ActionType type : conflictTypes) {
            conflictAction.type = type;
            when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(conflictAction));
            try {
                resource.deleteCdnSite(vmId, cdnDetail.siteId);
                fail();
            } catch (Vps4Exception e) {
                assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
            }
        }
    }

    @Test
    public void testUpdateCdnSiteCreatesAction() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        VmUpdateCdnRequest req = new VmUpdateCdnRequest();
        req.cacheLevel = "CACHING_OPTIMIZED";
        resource.updateCdnSite(vmId, cdnDetail.siteId, req);
        verify(actionService, times(1))
                .createAction(vmId, ActionType.MODIFY_CDN, "{}", userEmployee.getUsername());
    }

    @Test
    public void testUpdateCdnSiteExecutesCommand() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        VmUpdateCdnRequest req = new VmUpdateCdnRequest();
        req.cacheLevel = "CACHING_OPTIMIZED";
        resource.updateCdnSite(vmId, cdnDetail.siteId, req);
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(argument.capture());
        CommandGroupSpec cmdGroup = argument.getValue();
        assertEquals("Vps4ModifyCdnSite", cmdGroup.commands.get(0).command);
    }

    @Test
    public void testUpdateCdnSiteConflictingAction() {
        Action conflictAction = mock(Action.class);
        List<ActionType> conflictTypes = Arrays.asList(ActionType.DELETE_CDN, ActionType.MODIFY_CDN, ActionType.CREATE_CDN, ActionType.VALIDATE_CDN);
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);

        for (ActionType type : conflictTypes) {
            conflictAction.type = type;
            when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(conflictAction));
            try {
                resource.updateCdnSite(vmId, cdnDetail.siteId, new VmUpdateCdnRequest());
                fail();
            } catch (Vps4Exception e) {
                assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
            }
        }
    }

    @Test
    public void testCreateCdnSiteCreatesAction() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        VmCreateCdnRequest req = new VmCreateCdnRequest();
        req.cacheLevel = "CACHING_OPTIMIZED";
        resource.createCdnSite(vmId, req);
        verify(actionService, times(1))
                .createAction(vmId, ActionType.CREATE_CDN, "{}", userEmployee.getUsername());
    }

    @Test
    public void testCreateCdnSiteExecutesCommand() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        VmCreateCdnRequest req = new VmCreateCdnRequest();
        req.cacheLevel = "CACHING_OPTIMIZED";
        resource.createCdnSite(vmId, req);
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(argument.capture());
        CommandGroupSpec cmdGroup = argument.getValue();
        assertEquals("Vps4SubmitCdnCreation", cmdGroup.commands.get(0).command);
    }

    @Test
    public void testCreateCdnSiteConflictingAction() {
        Action conflictAction = mock(Action.class);
        List<ActionType> conflictTypes = Arrays.asList(ActionType.DELETE_CDN, ActionType.MODIFY_CDN, ActionType.CREATE_CDN, ActionType.VALIDATE_CDN);
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);

        for (ActionType type : conflictTypes) {
            conflictAction.type = type;
            when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(conflictAction));
            try {
                resource.createCdnSite(vmId, new VmCreateCdnRequest());
                fail();
            } catch (Vps4Exception e) {
                assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
            }
        }
    }

    @Test
    public void testCreateCdnSiteReachesLimit() {
        VmCdnSite fillerSite = new VmCdnSite();
        when(cdnDataService.getActiveCdnSitesOfVm(eq(vmId))).thenReturn(Arrays.asList(fillerSite, fillerSite, fillerSite, fillerSite, fillerSite));

        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        try {
            resource.createCdnSite(vmId, new VmCreateCdnRequest());
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SIZE_LIMIT_REACHED", e.getId());
        }
    }

    @Test
    public void testClearCacheCdnSiteCreatesAction() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        resource.clearCdnSiteCache(vmId, cdnDetail.siteId);
        verify(actionService, times(1))
                .createAction(vmId, ActionType.CLEAR_CDN_CACHE, "{}", userEmployee.getUsername());
    }

    @Test
    public void testClearCacheCdnSiteExecutesCommand() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        resource.clearCdnSiteCache(vmId, cdnDetail.siteId);
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(argument.capture());
        CommandGroupSpec cmdGroup = argument.getValue();
        assertEquals("Vps4ClearCdnCache", cmdGroup.commands.get(0).command);
    }

    @Test
    public void testClearCacheConflictingAction() {
        Action conflictAction = mock(Action.class);
        List<ActionType> conflictTypes = Arrays.asList(ActionType.CLEAR_CDN_CACHE);
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);

        for (ActionType type : conflictTypes) {
            conflictAction.type = type;
            when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(conflictAction));
            try {
                resource.clearCdnSiteCache(vmId, cdnDetail.siteId);
                fail();
            } catch (Vps4Exception e) {
                assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
            }
        }
    }

    @Test
    public void testValidateCdnSiteCreatesAction() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        resource.validateCdnSite(vmId, cdnDetail.siteId);
        verify(actionService, times(1))
                .createAction(vmId, ActionType.VALIDATE_CDN, "{}", userEmployee.getUsername());
    }

    @Test
    public void testValidateCdnSiteExecutesCommand() {
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);
        resource.validateCdnSite(vmId, cdnDetail.siteId);
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(argument.capture());
        CommandGroupSpec cmdGroup = argument.getValue();
        assertEquals("Vps4ValidateCdn", cmdGroup.commands.get(0).command);
    }

    @Test
    public void testValidateCdnConflictingAction() {
        Action conflictAction = mock(Action.class);
        List<ActionType> conflictTypes = Arrays.asList(ActionType.DELETE_CDN, ActionType.MODIFY_CDN, ActionType.CREATE_CDN, ActionType.VALIDATE_CDN);
        resource = new CdnResource(userEmployee, vmResource, creditService, cdnService, cdnDataService,
                cryptography, actionService, commandService);

        for (ActionType type : conflictTypes) {
            conflictAction.type = type;
            when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(conflictAction));
            try {
                resource.validateCdnSite(vmId, cdnDetail.siteId);
                fail();
            } catch (Vps4Exception e) {
                assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
            }
        }
    }
}
