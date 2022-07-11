package com.godaddy.vps4.web.monitoring;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.orchestration.monitoring.Vps4AddDomainMonitoring;
import com.godaddy.vps4.orchestration.panopta.Vps4ReplaceDomainMonitoring;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaMetricId;
import com.godaddy.vps4.panopta.PanoptaMetricMapper;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VmDomainMonitoringResourceTest {

    private GDUser user = GDUserMock.createShopper();
    private VmResource vmResource = mock(VmResource.class);
    private ActionService actionService = mock(ActionService.class);
    private CommandService commandService = mock(CommandService.class);
    private CreditService creditService = mock(CreditService.class);
    private PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    private PanoptaService panoptaService = mock(PanoptaService.class);
    private PanoptaMetricMapper panoptaMetricMapper = mock(PanoptaMetricMapper.class);

    private VmDomainMonitoringResource resource;

    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
    VmDomainMonitoringResource.AddDomainMonitoringRequest request;
    VmDomainMonitoringResource.ReplaceDomainMonitoringRequest replaceDomainToMonitoringRequest;

    private long hfsVmId = 23L;
    private Vm hfsVm;
    private ResultSubset<Action> actions;

    private VirtualMachineCredit createMockCredit() {
        Map<String, String> managedPlanFeatures = new HashMap<>();
        managedPlanFeatures.put(ECommCreditService.PlanFeatures.MANAGED_LEVEL.toString(), "2");
        return new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(AccountStatus.ACTIVE)
                .withPlanFeatures(managedPlanFeatures)
                .withShopperID("shopper")
                .build();
    }
    private VirtualMachine createMockVm() {
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.hfsVmId = hfsVmId;
        vm.vmId = vmId;
        vm.orionGuid = orionGuid;
        Image vmImage = new Image();
        vmImage.operatingSystem = Image.OperatingSystem.LINUX;
        vm.image = vmImage;
        return vm;
    }

    @Before
    public void setUp() throws PanoptaServiceException {

        request = new VmDomainMonitoringResource.AddDomainMonitoringRequest();
        request.additionalFqdn = "domain.test";

        replaceDomainToMonitoringRequest = new VmDomainMonitoringResource.ReplaceDomainMonitoringRequest();
        replaceDomainToMonitoringRequest.protocol = VmDomainMonitoringResource.FqdnProtocol.HTTP;

        VirtualMachineCredit managedVmCredit = createMockCredit();
        VirtualMachine vm = createMockVm();

        hfsVm = mock(Vm.class);
        hfsVm.status = "ACTIVE";

        PanoptaMetricId metric = new PanoptaMetricId();
        metric.typeId = 81;

        Action vmAction = mock(Action.class);
        actions = new ResultSubset<>(Collections.emptyList(), 0);

        when(vmResource.getVm(vmId)).thenReturn(vm);

        when(vmResource.getVmFromVmVertical(hfsVmId)).thenReturn(hfsVm);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(managedVmCredit);
        when(panoptaService.getNetworkIdOfAdditionalFqdn(vmId, request.additionalFqdn))
                .thenReturn(metric);
        when(panoptaMetricMapper.getVmMetric(metric.typeId))
                .thenReturn(VmMetric.HTTPS);

        when(actionService.getAction(anyLong())).thenReturn(vmAction);
        when(actionService.getActionList(any())).thenReturn(actions);

        when(commandService.executeCommand(any())).thenReturn(new CommandState());
        resource = new VmDomainMonitoringResource(user, vmResource, actionService, commandService,
                creditService, panoptaDataService, panoptaService, panoptaMetricMapper);
    }

    @Test
    public void testGetAdditionalFqdnMetric() {
        List<PanoptaMetricId> fqdnMetrics = resource.getFqdnMetrics(vmId);

        verify(vmResource).getVm(vmId);
        verify(panoptaService).getAdditionalFqdnMetricIds(vmId);
        Assert.assertNotNull(fqdnMetrics);
    }

    @Test
    public void createsAddDomainMonitoringAction() {
        resource.addDomainMonitoring(vmId, request);
        verify(actionService).createAction(vmId, ActionType.ADD_DOMAIN_MONITORING, "{}", user.getUsername());
    }

    @Test
    public void executesAddDomainMonitoringCommand() {
        resource.addDomainMonitoring(vmId, request);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(argument.capture());
        CommandGroupSpec cmdGroup = argument.getValue();
        assertEquals("Vps4AddDomainMonitoring", cmdGroup.commands.get(0).command);
    }

    @Test
    public void executesAddDomainMonitoringCommandWithOverrideProtocol() {
        request.overrideProtocol = VmDomainMonitoringResource.FqdnProtocol.HTTPS;
        resource.addDomainMonitoring(vmId, request);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(argument.capture());
        CommandGroupSpec cmdGroup = argument.getValue();
        Vps4AddDomainMonitoring.Request request = (Vps4AddDomainMonitoring.Request) cmdGroup.commands.get(0).request;
        assertEquals("Vps4AddDomainMonitoring", cmdGroup.commands.get(0).command);
        assertEquals("HTTPS", request.overrideProtocol);
    }

    @Test
    public void errorsAddDomainMonitoringIfConflictingActionPending() {
        Action conflictAction = mock(Action.class);
        List<ActionType> conflictTypes = Arrays.asList(ActionType.DELETE_DOMAIN_MONITORING,
                ActionType.ADD_DOMAIN_MONITORING, ActionType.ADD_MONITORING);

        for (ActionType type : conflictTypes) {
            conflictAction.type = type;
            when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(conflictAction));
            try {
                resource.addDomainMonitoring(vmId, request);
                fail();
            } catch (Vps4Exception e) {
                assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
            }
        }
    }

    @Test
    public void errorsAddDomainMonitoringIfActiveFqdnAlreadyExists() {
        when(panoptaDataService.getPanoptaActiveAdditionalFqdns(vmId)).thenReturn(Arrays.asList("domain1.test",
                "domain.test"));
        try {
            resource.addDomainMonitoring(vmId, request);
            Assert.fail();
        } catch (Vps4Exception e) {
            assertEquals("DUPLICATE_FQDN", e.getId());
        }
    }

    @Test
    public void errorsIfVmIsManagedAndHas5OrMoreDomain() {
        when(panoptaDataService.getPanoptaActiveAdditionalFqdns(vmId)).thenReturn(Arrays.asList("domain1.test",
                "domain2.test", "domain3.test", "domain4.test", "domain5.test"));
        try {
            resource.addDomainMonitoring(vmId, request);
            Assert.fail();
        } catch (Vps4Exception e) {
            assertEquals("DOMAIN_LIMIT_REACHED", e.getId());
        }
    }

    @Test
    public void errorsIfAdditionalFqdnFieldIsEmpty() {
        request.additionalFqdn = null;
        try {
            resource.addDomainMonitoring(vmId, request);
            Assert.fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_ADDITIONAL_FQDN", e.getId());
        }
    }

    @Test
    public void errorsIfVmIsSelfManagedAndHas1OrMoreDomain() {
        Map<String, String> selfManagedPlanFeatures = new HashMap<>();
        selfManagedPlanFeatures.put(ECommCreditService.PlanFeatures.MANAGED_LEVEL.toString(), "0");

        VirtualMachineCredit selfManagedVmCredit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(AccountStatus.ACTIVE)
                .withPlanFeatures(selfManagedPlanFeatures)
                .withShopperID("shopper")
                .build();
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(selfManagedVmCredit);

        when(panoptaDataService.getPanoptaActiveAdditionalFqdns(vmId)).thenReturn(Arrays.asList("domain.test"));
        request.additionalFqdn = "domain2.test";
        try {
            resource.addDomainMonitoring(vmId, request);
            Assert.fail();
        } catch (Vps4Exception e) {
            assertEquals("DOMAIN_LIMIT_REACHED", e.getId());
        }
    }

    @Test
    public void createsDeleteDomainMonitoringAction() {
        resource.deleteDomainMonitoring(vmId, "additionalfqdn.fake");
        verify(actionService).createAction(vmId, ActionType.DELETE_DOMAIN_MONITORING, "{}", user.getUsername());
    }

    @Test
    public void executesDeleteDomainMonitoringCommand() {
        resource.deleteDomainMonitoring(vmId, "additionalfqdn.fake");

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(argument.capture());
        CommandGroupSpec cmdGroup = argument.getValue();
        assertEquals("Vps4RemoveDomainMonitoring", cmdGroup.commands.get(0).command);
    }

    @Test
    public void errorsDeleteDomainMonitoringIfConflictingActionPending() {
        Action conflictAction = mock(Action.class);
        List<ActionType> conflictTypes = Arrays.asList(ActionType.DELETE_DOMAIN_MONITORING,
                ActionType.ADD_MONITORING, ActionType.ADD_MONITORING);

        for (ActionType type : conflictTypes) {
            conflictAction.type = type;
            when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(conflictAction));
            try {
                resource.deleteDomainMonitoring(vmId, "additionalfqdn.fake");
                fail();
            } catch (Vps4Exception e) {
                assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
            }
        }
    }

    @Test
    public void createsReplaceDomainMonitoringAction() throws PanoptaServiceException {
        resource.replaceDomainMonitoring(vmId, "domain.test", replaceDomainToMonitoringRequest);
        verify(actionService).createAction(vmId, ActionType.REPLACE_DOMAIN_MONITORING, "{}", user.getUsername());
    }

    @Test
    public void executesReplaceDomainMonitoringCommand() throws PanoptaServiceException {
        resource.replaceDomainMonitoring(vmId, "domain.test", replaceDomainToMonitoringRequest);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(argument.capture());
        CommandGroupSpec cmdGroup = argument.getValue();
        assertEquals("Vps4ReplaceDomainMonitoring", cmdGroup.commands.get(0).command);
        Vps4ReplaceDomainMonitoring.Request req = (Vps4ReplaceDomainMonitoring.Request) cmdGroup.commands.get(0).request;
        assertEquals(replaceDomainToMonitoringRequest.protocol.toString(), req.protocol);
        assertEquals(vmId, req.vmId);
        assertEquals("domain.test", req.additionalFqdn);
    }

    @Test
    public void errorsReplaceDomainMonitoringIfConflictingActionPending() throws PanoptaServiceException {
        Action conflictAction = mock(Action.class);
        List<ActionType> conflictTypes = Arrays.asList(ActionType.ADD_DOMAIN_MONITORING,
                ActionType.ADD_MONITORING, ActionType.DELETE_DOMAIN_MONITORING);

        for (ActionType type : conflictTypes) {
            conflictAction.type = type;
            when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(conflictAction));
            try {
                resource.replaceDomainMonitoring(vmId, "domain.test", replaceDomainToMonitoringRequest);
                fail();
            } catch (Vps4Exception e) {
                assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
            }
        }
    }

    @Test
    public void errorsReplaceDomainMonitoringIfProtocolIsNull() throws PanoptaServiceException {
        replaceDomainToMonitoringRequest.protocol = null;
        try {
            resource.replaceDomainMonitoring(vmId,"domain.test", replaceDomainToMonitoringRequest);
            Assert.fail();
        } catch (Vps4Exception e) {
            assertEquals("PROTOCOL_INVALID", e.getId());
        }
    }

    @Test
    public void doesNotCreateVmActionIfProtocolIsTheSame() throws PanoptaServiceException {
        replaceDomainToMonitoringRequest.protocol = VmDomainMonitoringResource.FqdnProtocol.HTTPS;
        resource.replaceDomainMonitoring(vmId,"domain.test", replaceDomainToMonitoringRequest);
        verify(actionService, never()).createAction(vmId, ActionType.REPLACE_DOMAIN_MONITORING, "{}", user.getUsername());
    }

    @Test
    public void errorsReplaceDomainMonitoringIfProtocolIsNotFound() throws PanoptaServiceException {
        when(panoptaService.getNetworkIdOfAdditionalFqdn(vmId, "domain.test"))
                .thenReturn(null);
        try {
            resource.replaceDomainMonitoring(vmId,"domain.test", replaceDomainToMonitoringRequest);
            Assert.fail();
        } catch (Vps4Exception e) {
            assertEquals("METRIC_NOT_FOUND", e.getId());
        }
    }
}
