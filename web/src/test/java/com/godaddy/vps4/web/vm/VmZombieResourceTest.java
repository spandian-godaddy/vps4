package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmZombieResourceTest {

    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    CreditService creditService = mock(CreditService.class);
    CommandService commandService = mock(CommandService.class);
    ActionService actionService = mock(ActionService.class);
    VmActionResource vmActionResource = mock(VmActionResource.class);
    SchedulerWebService schedulerWebService = mock(SchedulerWebService.class);
    ScheduledJobService scheduledJobService = mock(ScheduledJobService.class);
    Config config = mock(Config.class);
    ScheduledJob scheduledJob = mock(ScheduledJob.class);
    ScheduledJob scheduledJob2 = mock(ScheduledJob.class);
    VirtualMachine testVm;
    VirtualMachineCredit oldCredit;
    VirtualMachineCredit newCredit;
    List<ScheduledJob> scheduledJobs;
    VmZombieResource vmZombieResource;
    UUID jobId = UUID.randomUUID();
    Instant nextRun = Instant.now().minus(7, ChronoUnit.DAYS);
    JobRequest jobRequest = mock(JobRequest.class);
    SchedulerJobDetail job;


    private GDUser user;

    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());

        testVm = new VirtualMachine();
        testVm.vmId = UUID.randomUUID();
        testVm.orionGuid = UUID.randomUUID();
        testVm.canceled = Instant.now().plus(7, ChronoUnit.DAYS);
        testVm.validUntil = Instant.MAX;
        when(virtualMachineService.getVirtualMachine(testVm.vmId)).thenReturn(testVm);

        oldCredit = createOldCredit(testVm, AccountStatus.REMOVED);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(oldCredit);

        UUID newOrionGuid = UUID.randomUUID();
        newCredit = createNewCredit(oldCredit, newOrionGuid);
        when(creditService.getVirtualMachineCredit(newOrionGuid)).thenReturn(newCredit);

        Action testAction = new Action(123L, testVm.vmId, ActionType.CANCEL_ACCOUNT, null, null, null,
                ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(), null);

        when(actionService.getAction(anyLong())).thenReturn(testAction);

        vmZombieResource =
                new VmZombieResource(virtualMachineService, creditService, commandService, user, actionService,
                        vmActionResource, schedulerWebService, scheduledJobService, config);
        job = new SchedulerJobDetail(jobId, nextRun, jobRequest, false);
        scheduledJobs = new ArrayList<>();
        scheduledJobs.add(scheduledJob);
        scheduledJobs.add(scheduledJob2);
        scheduledJob.id = jobId;
        when(scheduledJobService.getScheduledJobsByType(eq(testVm.vmId),
                eq(ScheduledJob.ScheduledJobType.ZOMBIE))).thenReturn(scheduledJobs);
        when(config.get(eq("vps4.zombie.cleanup.waittime"))).thenReturn("21");
        when(schedulerWebService.getJob(eq("vps4"), eq("zombie"), eq(jobId))).thenReturn(job);
    }

    private VirtualMachineCredit createVmCredit(UUID orionGuid, AccountStatus accountStatus, String controlPanel,
                                                int monitoring, int managedLevel, int tier, String os, Instant provisionDate, int cdnWaf) {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put("tier", String.valueOf(tier));
        planFeatures.put("managed_level", String.valueOf(managedLevel));
        planFeatures.put("control_panel_type", String.valueOf(controlPanel));
        planFeatures.put("monitoring", String.valueOf(monitoring));
        planFeatures.put("operatingsystem", os);
        planFeatures.put("cdnwaf", String.valueOf(cdnWaf));

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

    private VirtualMachineCredit createNewCredit(VirtualMachineCredit oldCredit, UUID newOrionGuid) {
        return createVmCredit(
                newOrionGuid, AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
                oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), null, 0);
    }

    private VirtualMachineCredit createOldCredit(VirtualMachine testVm, AccountStatus accountStatus) {
        return createVmCredit(
                testVm.orionGuid, accountStatus, "cpanel", 0, 0, 10, "linux", null, 0);
    }

    @Test
    public void testReviveZombieVm() {
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
        verify(commandService, times(1)).executeCommand(anyObject());
        verify(actionService, times(1)).createAction(Matchers.eq(testVm.vmId),
                Matchers.eq(ActionType.RESTORE_ACCOUNT), anyObject(), anyString());
    }

    @Test
    public void testOldCreditNotRemoved() {
        oldCredit = createOldCredit(testVm, AccountStatus.ACTIVE);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(oldCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_STATUS_NOT_REMOVED", e.getId());
        }
    }

    @Test
    public void testNewCreditInUse() {
        newCredit = createVmCredit(
                newCredit.getEntitlementId(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
                oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), Instant.now(), 0);
        when(creditService.getVirtualMachineCredit(newCredit.getEntitlementId())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CREDIT_ALREADY_IN_USE", e.getId());
        }
    }

    @Test
    public void testControlPanelsDontMatch() {
        newCredit = createVmCredit(
                newCredit.getEntitlementId(), AccountStatus.ACTIVE, "myh", oldCredit.getMonitoring(),
                oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), null, 0);
        when(creditService.getVirtualMachineCredit(newCredit.getEntitlementId())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CONTROL_PANEL_MISMATCH", e.getId());
        }
    }

    @Test
    public void testManagedLevelsDontMatch() {
        newCredit = createVmCredit(
                newCredit.getEntitlementId(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
                2, oldCredit.getTier(), oldCredit.getOperatingSystem(), null, 0);
        when(creditService.getVirtualMachineCredit(newCredit.getEntitlementId())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("MANAGED_LEVEL_MISMATCH", e.getId());
        }
    }

    @Test
    public void testAllowManagedLevelMismatchForLegacyManaged() {
        oldCredit = createVmCredit(
            oldCredit.getEntitlementId(), oldCredit.getAccountStatus(), oldCredit.getControlPanel(), oldCredit.getMonitoring(),
            1, oldCredit.getTier(), oldCredit.getOperatingSystem(), null, 0);
        newCredit = createVmCredit(
                newCredit.getEntitlementId(), AccountStatus.ACTIVE, oldCredit.getControlPanel(),  oldCredit.getMonitoring(),
                0, oldCredit.getTier(), oldCredit.getOperatingSystem(), null, 0);
        when(creditService.getVirtualMachineCredit(newCredit.getEntitlementId())).thenReturn(newCredit);
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
        verify(commandService, times(1)).executeCommand(anyObject());
    }

    @Test
    public void testMonitoringsDontMatch() {
        newCredit = createVmCredit(
                newCredit.getEntitlementId(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), 1,
                oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), null, 0);
        when(creditService.getVirtualMachineCredit(newCredit.getEntitlementId())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("MONITORING_MISMATCH", e.getId());
        }
    }

    @Test
    public void testAllowMonitoringMismatchForLegacyManaged() {
        oldCredit = createVmCredit(
            oldCredit.getEntitlementId(), oldCredit.getAccountStatus(), oldCredit.getControlPanel(), 1,
            oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), null, 0);
        newCredit = createVmCredit(
                newCredit.getEntitlementId(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), 0,
                oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), null, 0);
        when(creditService.getVirtualMachineCredit(newCredit.getEntitlementId())).thenReturn(newCredit);
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
        verify(commandService, times(1)).executeCommand(anyObject());
    }

    @Test
    public void testOperatingSystemsDontMatch() {
        newCredit = createVmCredit(
                newCredit.getEntitlementId(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
                oldCredit.getManagedLevel(), oldCredit.getTier(), "windows", null, 0);
        when(creditService.getVirtualMachineCredit(newCredit.getEntitlementId())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("OPERATING_SYSTEM_MISMATCH", e.getId());
        }
    }

    @Test
    public void testTiersDontMatch() {
        newCredit = createVmCredit(
                newCredit.getEntitlementId(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
                oldCredit.getManagedLevel(), 20, oldCredit.getOperatingSystem(), null, 0);
        when(creditService.getVirtualMachineCredit(newCredit.getEntitlementId())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("TIER_MISMATCH", e.getId());
        }
    }

    @Test
    public void testCdnAddOnDontMatch() {
        newCredit = createVmCredit(
                newCredit.getEntitlementId(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
                oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), null, 2);
        when(creditService.getVirtualMachineCredit(newCredit.getEntitlementId())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getEntitlementId());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CDN_MISMATCH", e.getId());
        }
    }

    @Test
    public void zombieVmKicksoffOrchEngineCommand() {
        vmZombieResource.zombieVm(testVm.vmId);
        verify(commandService, times(1)).executeCommand(anyObject());
    }

    @Test
    public void zombieVmGetsListOfCurrentlyInProgressVmActions() {
        vmZombieResource.zombieVm(testVm.vmId);
        verify(actionService, times(1)).getIncompleteActions(testVm.vmId);
    }

    @Test
    public void zombieVmCancelsInProgressVmActions() {
        Action[] inCompleteActions = {mock(Action.class), mock(Action.class), mock(Action.class)};
        when(actionService.getIncompleteActions(testVm.vmId)).thenReturn(Arrays.asList(inCompleteActions));

        vmZombieResource.zombieVm(testVm.vmId);
        verify(vmActionResource, times(3)).cancelVmAction(eq(testVm.vmId), anyInt());
    }

    @Test
    public void testCreditNotRemoved() {
        oldCredit = createOldCredit(testVm, AccountStatus.ACTIVE);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(oldCredit);
        try {
            vmZombieResource.zombieVm(testVm.vmId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_STATUS_NOT_REMOVED", e.getId());
        }
    }

    @Test
    public void testRescheduleZombieVmDeleteInvokesCommand() {
        testVm.canceled = Instant.now().minus(10, ChronoUnit.MINUTES);

        vmZombieResource.rescheduleZombieVmDelete(testVm.vmId);
        ArgumentCaptor<CommandGroupSpec>
                commandGroupSpecArgumentCaptor = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(2)).executeCommand(commandGroupSpecArgumentCaptor.capture());
        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        assertSame("RescheduleZombieVmCleanup", commandGroupSpec.commands.get(0).command);
    }

    @Test(expected = Vps4Exception.class)
    public void doesNotRescheduleZombieVmDeleteIfVmIsActive() {

        try {
            vmZombieResource.rescheduleZombieVmDelete(testVm.vmId);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(
                    "Vm status not as expected. Vm should be in zombie status (canceled and scheduled for deletion)"));
            throw e;
        }
    }

    @Test(expected = Vps4Exception.class)
    public void doesNotRescheduleDeletionIfNoPreviousCleanupJobExists() {
        testVm.canceled = Instant.now().minus(10, ChronoUnit.MINUTES);
        when(scheduledJobService.getScheduledJobsByType(eq(testVm.vmId),
                eq(ScheduledJob.ScheduledJobType.ZOMBIE))).thenReturn(Collections.emptyList());

        try {
            vmZombieResource.rescheduleZombieVmDelete(testVm.vmId);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(
                    "Expected 1 zombie cleanup job scheduled, returned 0"));
            throw e;
        }
    }

    @Test
    public void deletesExtraZombieCleanupJobsOnReschedule() {
        testVm.canceled = Instant.now().minus(10, ChronoUnit.MINUTES);

        vmZombieResource.rescheduleZombieVmDelete(testVm.vmId);
        ArgumentCaptor<CommandGroupSpec>
                commandGroupSpecArgumentCaptor = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(2)).executeCommand(commandGroupSpecArgumentCaptor.capture());
        List<CommandGroupSpec> commandGroupSpec = commandGroupSpecArgumentCaptor.getAllValues();
        assertSame("Vps4DeleteExtraScheduledZombieJobsForVm", commandGroupSpec.get(0).commands.get(0).command);
    }

    @Test
    public void getsScheduledZombieVmDeleteJob() {
        testVm.canceled = Instant.now().minus(10, ChronoUnit.MINUTES);

        List<SchedulerJobDetail> zombieCleanupJobs = vmZombieResource.getScheduledZombieVmDelete(testVm.vmId);

        assertNotNull(zombieCleanupJobs);
        assertEquals(2, zombieCleanupJobs.size());
        SchedulerJobDetail zombieCleanupJob = zombieCleanupJobs.get(0);
        assertEquals(jobId, zombieCleanupJob.id);
        assertEquals(nextRun, zombieCleanupJob.nextRun);
        assertFalse(zombieCleanupJob.isPaused);
    }

    @Test
    public void classHasExpectedRoles() {
        GDUser.Role[] expectedRoles = new GDUser.Role[] {GDUser.Role.ADMIN};
        Assert.assertArrayEquals(expectedRoles, VmZombieResource.class.getAnnotation(RequiresRole.class).roles());
    }

    @Test
    public void zombieReviveHasExpectedRoles() throws NoSuchMethodException {
        GDUser.Role[] expectedRoles = new GDUser.Role[] {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.HS_AGENT,
                GDUser.Role.SUSPEND_AUTH, GDUser.Role.C3_OTHER};
        Assert.assertArrayEquals(expectedRoles, VmZombieResource.class.getMethod("reviveZombieVm", UUID.class, UUID.class)
                                                                      .getAnnotation(RequiresRole.class).roles());
    }
}
