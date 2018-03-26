package com.godaddy.vps4.orchestration.phase2;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Function;

import javax.sql.DataSource;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.orchestration.scheduler.ScheduleAutomaticBackupRetry;
import com.godaddy.vps4.orchestration.vm.Vps4RecordScheduledJobForVm;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.orchestration.snapshot.Vps4DestroySnapshot;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.orchestration.snapshot.WaitForSnapshotAction;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.snapshot.SnapshotAction;

@SuppressWarnings("unchecked")
public class Vps4SnapshotVmTest {

    static Injector injector;
    private Vps4SnapshotVm command;
    private CommandContext context;
    private Vps4SnapshotVm.Request request;
    private Vps4SnapshotVm.Request automaticRequest;
    private gdg.hfs.vhfs.snapshot.SnapshotAction hfsAction;
    private gdg.hfs.vhfs.snapshot.Snapshot hfsSnapshot;
    private UUID orionGuid;
    private UUID vps4SnapshotId;
    private UUID vps4AutomaticSnapshotId;
    private long vps4UserId;
    private UUID vps4SnapshotIdToBeDeprecated;
    private long vps4SnapshotActionId;
    private long vps4AutomaticSnapshotActionId;
    private long hfsActionId = 12345L;
    private long hfsSnapshotId = 4567L;
    private String hfsImageId = "nocfoxid";
    private SnapshotService spySnapshotService;


    @Inject Vps4UserService vps4UserService;
    @Inject ProjectService projectService;
    @Inject VirtualMachineService virtualMachineService;
    @Inject SnapshotService snapshotService;
    @Inject gdg.hfs.vhfs.snapshot.SnapshotService hfsSnapshotService;
    @Inject @SnapshotActionService ActionService actionService;
    @Inject Config config;

    @Captor ArgumentCaptor<Function<CommandContext, Void>> snapshotCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, Void>> cancelSnapshotCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, UUID>> markOldestSnapshotCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, SnapshotAction>> snapshotActionCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, gdg.hfs.vhfs.snapshot.Snapshot>> hfsSnapshotCaptor;
    @Captor private ArgumentCaptor<Vps4RecordScheduledJobForVm.Request> recordJobArgumentCaptor;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
                new DatabaseModule(),
                new SecurityModule(),
                new SnapshotModule(),
                new Vps4ExternalsModule(),
                new Vps4SnapshotTestModule()
        );
    }

    @Before
    public void setUpTest() {
        MockitoAnnotations.initMocks(this);
        injector.injectMembers(this);

        spySnapshotService = spy(snapshotService);
        command = new Vps4SnapshotVm(actionService, hfsSnapshotService, spySnapshotService, config);
        addTestSqlData();
        context = setupMockContext();
        request = getCommandRequest(vps4SnapshotActionId, vps4SnapshotId, SnapshotType.ON_DEMAND);
        automaticRequest = getCommandRequest(vps4AutomaticSnapshotActionId, vps4AutomaticSnapshotId, SnapshotType.AUTOMATIC);
    }

    private Vps4SnapshotVm.Request getCommandRequest(long snapshotActionId, UUID snapshotId, SnapshotType snapshotType) {
        Vps4SnapshotVm.Request req = new Vps4SnapshotVm.Request();
        req.actionId = snapshotActionId;
        req.vps4SnapshotId = snapshotId;
        req.orionGuid = orionGuid;
        req.vps4UserId = vps4UserId;
        req.snapshotType = snapshotType;
        return req;
    }

    private CommandContext setupMockContext() {
        CommandContext mockContext = mock(CommandContext.class);
        when(mockContext.getId()).thenReturn(UUID.randomUUID());

        hfsAction = new gdg.hfs.vhfs.snapshot.SnapshotAction();
        hfsAction.actionId = hfsActionId;
        hfsAction.snapshotId = hfsSnapshotId;

        when(mockContext.execute(eq("Vps4SnapshotVm"), any(Function.class), eq(SnapshotAction.class))).thenReturn(hfsAction);
        when(mockContext.execute(eq("MarkOldestSnapshotForDeprecation" + orionGuid), any(Function.class), eq(UUID.class))).thenReturn(vps4SnapshotIdToBeDeprecated);
        when(mockContext.execute(eq(WaitForSnapshotAction.class), eq(hfsAction))).thenReturn(hfsAction);

        hfsSnapshot = new gdg.hfs.vhfs.snapshot.Snapshot();
        hfsSnapshot.imageId = hfsImageId;
        when(mockContext.execute(eq("GetHFSSnapshot"), any(Function.class), eq(gdg.hfs.vhfs.snapshot.Snapshot.class)))
                .thenReturn(hfsSnapshot);

        return mockContext;
    }

    private void addTestSqlData() {
        vps4UserId = SqlTestData.insertUser(vps4UserService).getId();
        Project project = SqlTestData.insertProject(projectService, vps4UserService);
        VirtualMachine vm = SqlTestData.insertVm(virtualMachineService, vps4UserService);
        orionGuid = vm.orionGuid;
        vps4SnapshotId = SqlTestData.insertSnapshot(snapshotService, vm.vmId, project.getProjectId(), SnapshotType.ON_DEMAND);
        vps4AutomaticSnapshotId = SqlTestData.insertSnapshot(snapshotService, vm.vmId, project.getProjectId(), SnapshotType.AUTOMATIC);
        vps4SnapshotIdToBeDeprecated = SqlTestData.insertSnapshotWithStatus(
                snapshotService, vm.vmId, project.getProjectId(), SnapshotStatus.LIVE, SnapshotType.ON_DEMAND);
        vps4SnapshotActionId = SqlTestData.insertSnapshotAction(actionService, vps4UserService, vps4SnapshotId);
        vps4AutomaticSnapshotActionId = SqlTestData.insertSnapshotAction(actionService, vps4UserService, vps4AutomaticSnapshotId);
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(
                injector.getInstance(DataSource.class), injector.getInstance(Vps4UserService.class));
    }

    @Test
    public void marksTheOldSnapshotStatusToDeprecating() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("MarkOldestSnapshotForDeprecation" + request.orionGuid),
                markOldestSnapshotCaptor.capture(),
                eq(UUID.class));

        Function<CommandContext, UUID> lambda = markOldestSnapshotCaptor.getValue();
        lambda.apply(context);
        verify(spySnapshotService, times(1)).markOldestSnapshotForDeprecation(request.orionGuid, SnapshotType.ON_DEMAND);
    }

    @Test
    public void cancelsOldErroredSnapshots() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("CancelErroredSnapshots"), cancelSnapshotCaptor.capture(), eq(Void.class));

        Function<CommandContext, Void> lambda = cancelSnapshotCaptor.getValue();
        lambda.apply(context);
        verify(spySnapshotService, times(1)).cancelErroredSnapshots(request.orionGuid, SnapshotType.ON_DEMAND);

    }

    @Test
    public void kicksOffAHfsRequestToSnapshotVm() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("Vps4SnapshotVm"), snapshotActionCaptor.capture(), eq(SnapshotAction.class));
    }

    @Test
    public void changesSnapshotStatusToInProgress() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("MarkSnapshotInProgress" + vps4SnapshotId), snapshotCaptor.capture(), eq(Void.class));

        Function<CommandContext, Void> lambda = snapshotCaptor.getValue();
        lambda.apply(context);
        verify(spySnapshotService, times(1)).markSnapshotInProgress(eq(vps4SnapshotId));
    }

    @Test
    public void updatesTheSnapshotWithTheHfsSnapshotId() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("UpdateHfsSnapshotId" + request.vps4SnapshotId), snapshotCaptor.capture(), eq(Void.class));

        Function<CommandContext, Void> lambda = snapshotCaptor.getValue();
        lambda.apply(context);
        verify(spySnapshotService, times(1)).updateHfsSnapshotId(eq(vps4SnapshotId), eq(hfsSnapshotId));
    }

    @Test
    public void waitsOnTheCompletionOfTheHfsAction() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq(WaitForSnapshotAction.class), eq(hfsAction));
    }

    @Test
    public void marksSnapshotStatusAsComplete() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("MarkSnapshotLive" + vps4SnapshotId), snapshotCaptor.capture(), eq(Void.class));

        Function<CommandContext, Void> lambda = snapshotCaptor.getValue();
        lambda.apply(context);
        Assert.assertEquals(SnapshotStatus.LIVE, snapshotService.getSnapshot(vps4SnapshotId).status);
    }

    @Test
    public void queriesHfsForDetailsOfTheCreatedSnapshot() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("GetHFSSnapshot"), hfsSnapshotCaptor.capture(), eq(gdg.hfs.vhfs.snapshot.Snapshot.class));
    }

    @Test
    public void updatesTheSnapshotWithTheHfsImageId() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("UpdateHfsImageId" + vps4SnapshotId), snapshotCaptor.capture(), eq(Void.class));

        Function<CommandContext, Void> lambda = snapshotCaptor.getValue();
        lambda.apply(context);
        verify(spySnapshotService, times(1)).updateHfsImageId(eq(vps4SnapshotId), eq(hfsImageId));
    }

    @Test
    public void marksTheOldSnapshotAsDeprecated() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("MarkOldestSnapshotForDeprecation" + request.orionGuid), markOldestSnapshotCaptor.capture(), eq(UUID.class));

        Function<CommandContext, UUID> lambda = markOldestSnapshotCaptor.getValue();
        lambda.apply(context);
        verify(spySnapshotService, times(1)).markOldestSnapshotForDeprecation(request.orionGuid, SnapshotType.ON_DEMAND);
    }

    @Test
    public void onlyMarksSameTypeOfSnapshotDeprecated() {

        when(context.execute(eq("MarkOldestSnapshotForDeprecation" + orionGuid), any())).thenReturn((UUID) null);
        command.execute(context, request);
        // vps4SnapshotIdToBeDeprecated is an On Demand backup, and should not be
        // deprecated to make way for an Automatic backup.
        verify(spySnapshotService, times(0))
                .markSnapshotAsDeprecated(eq(vps4SnapshotIdToBeDeprecated));
    }

    @Test
    public void destroysTheOldSnapshot() {
        command.execute(context, request);
        verify(context, times(1))
            .execute(eq(Vps4DestroySnapshot.class), any(Vps4DestroySnapshot.Request.class));
    }

    @Test
    public void createsActionToTrackOldSnapshotDestroy() {
        command.execute(context, request);
        Assert.assertEquals(
                ActionType.DESTROY_SNAPSHOT, actionService.getActions(vps4SnapshotIdToBeDeprecated).get(0).type);
    }

    @Test
    public void errorInInitialRequestSetsStatusToError() {
        SnapshotAction execute = context.execute(eq("Vps4SnapshotVm"), any(Function.class), eq(SnapshotAction.class));
		when(execute).thenThrow(new RuntimeException("Error in initial request"));
        command.execute(context, request);

        verify(spySnapshotService, times(1)).markSnapshotErrored(eq(vps4SnapshotId));
        Assert.assertEquals(snapshotService.getSnapshot(vps4SnapshotId).status, SnapshotStatus.ERROR);
    }

    private void verifyBackupRescheduled(int numOfTimesCalled){
        when(context.execute(eq("Vps4SnapshotVm"), any(Function.class), eq(SnapshotAction.class))).thenThrow(new RuntimeException("Error in initial request"));
        try {
            command.execute(context, automaticRequest);
        }catch(RuntimeException rte){
            // Automatic backups throw a NoRetryException which is not raised through
            // the orchestration engine.
            Assert.fail("Should not have thrown a runtime exception");
        }
        verify(context, times(numOfTimesCalled)).execute(eq(ScheduleAutomaticBackupRetry.class), any(ScheduleAutomaticBackupRetry.Request.class));
    }

    @Test
    public void rescheduleWhenAutoSnapshotFails() {
        verifyBackupRescheduled(1);
    }

    @Test
    public void dontRescheduleWhenAutoSnapshotFailsAndAboveRetryLimit() {
        int retryLimit = Integer.valueOf(config.get("vps4.autobackup.failedBackupRetryLimit"));
        doReturn(retryLimit + 1).when(spySnapshotService).failedBackupsSinceSuccess(any(UUID.class), eq(SnapshotType.AUTOMATIC));
        verifyBackupRescheduled(0);
    }

    @Test
    public void errorInCreationProcessSetsStatusToError() {
        when(context.execute(eq(WaitForSnapshotAction.class), eq(hfsAction)))
                .thenThrow(new RuntimeException("Error in initial request"));
        command.execute(context, request);

        verify(spySnapshotService, times(1)).markSnapshotErrored(eq(vps4SnapshotId));
        Assert.assertEquals(snapshotService.getSnapshot(vps4SnapshotId).status, SnapshotStatus.ERROR);
    }

    @Test
    public void errorInCreationProcessSchedulesAnotherBackup() {
        when(context.execute(eq(WaitForSnapshotAction.class), eq(hfsAction)))
                .thenThrow(new RuntimeException("Error in initial request"));

        try {
            command.execute(context, automaticRequest);
        }catch(RuntimeException rte){
            // Automatic backups throw a NoRetryException which is not raised through
            // the orchestration engine.
            Assert.fail("Should not have thrown a runtime exception");
        }

        verify(context, times(1)).execute(eq(ScheduleAutomaticBackupRetry.class), any(ScheduleAutomaticBackupRetry.Request.class));
    }

    @Test
    public void errorInCreationRecordsScheduledJobId() {
        UUID retryJobId = UUID.randomUUID();
        when(context.execute(eq(WaitForSnapshotAction.class), eq(hfsAction)))
                .thenThrow(new RuntimeException("Error in initial request"));
        when(context.execute(eq(ScheduleAutomaticBackupRetry.class), any(ScheduleAutomaticBackupRetry.Request.class)))
                .thenReturn(retryJobId);

        try {
            command.execute(context, automaticRequest);
        }catch(RuntimeException rte){
            // Automatic backups throw a NoRetryException which is not raised through
            // the orchestration engine.
            Assert.fail("Should not have thrown a runtime exception");
        }

        verify(context, times(1))
                .execute(eq("RecordScheduledJobId"), eq(Vps4RecordScheduledJobForVm.class), recordJobArgumentCaptor.capture());
        Vps4RecordScheduledJobForVm.Request req = recordJobArgumentCaptor.getValue();
        Assert.assertEquals(retryJobId, req.jobId);
        Assert.assertEquals(ScheduledJob.ScheduledJobType.BACKUPS, req.jobType);
    }
}
