package com.godaddy.vps4.orchestration.phase2;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Function;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.orchestration.account.Vps4ProcessAccountCancellation;
import com.godaddy.vps4.orchestration.scheduler.ScheduleZombieVmCleanup;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4RecordScheduledJobForVm;
import com.godaddy.vps4.orchestration.vm.Vps4StopVm;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduledJob.jdbc.JdbcScheduledJobService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVmActionService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;

public class Vps4ProcessAccountCancellationTest {
    static Injector injector;
    Instant validUntil = Instant.now();
    private CommandContext context;
    private UUID vps4VmId;
    private UUID orionGuid;
    private VirtualMachine vm;
    private long hfsVmId = 4567;
    private long stopActionId = 1234;
    private VirtualMachineCredit virtualMachineCredit;
    private Vps4ProcessAccountCancellation.Request request;

    @Inject Vps4UserService vps4UserService;
    @Inject ProjectService projectService;
    @Inject VirtualMachineService vps4VmService;
    @Inject ActionService actionService;
    @Inject Vps4ProcessAccountCancellation command;

    @Captor private ArgumentCaptor<Function<CommandContext, Long>> calculateValidUntilLambdaCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, Long>> createStopVmActionLambdaCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, Long>> createCancelAccountActionLambdaCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, VirtualMachine>> getVirtualMachineLambdaCaptor;
    @Captor private ArgumentCaptor<VmActionRequest> actionRequestArgumentCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, Void>> markZombieLambdaCaptor;
    @Captor private ArgumentCaptor<ScheduleZombieVmCleanup.Request> zombieCleanupArgumentCaptor;
    @Captor private ArgumentCaptor<Vps4RecordScheduledJobForVm.Request> recordJobArgumentCaptor;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
                new DatabaseModule(),
                new SecurityModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ActionService.class).to(JdbcVmActionService.class);
                        bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
                        bind(ScheduledJobService.class).to(JdbcScheduledJobService.class);
                    }
                }
        );
    }

    @Before
    public void setUpTest() {
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);
        addTestSqlData();
        context = setupMockContext();
        virtualMachineCredit = getVirtualMachineCredit();
        request = new Vps4ProcessAccountCancellation.Request();
        request.virtualMachineCredit = virtualMachineCredit;
        request.setActionId(1);
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(
                injector.getInstance(DataSource.class), injector.getInstance(Vps4UserService.class));
    }

    private VirtualMachineCredit getVirtualMachineCredit() {
        VirtualMachineCredit credit = new VirtualMachineCredit();
        credit.productId = vps4VmId;
        credit.orionGuid = orionGuid;
        return credit;
    }

    private void addTestSqlData() {
        SqlTestData.insertUser(vps4UserService);
        SqlTestData.insertProject(projectService, vps4UserService);
        vm = SqlTestData.insertVm(vps4VmService, vps4UserService);
        vps4VmId = vm.vmId;
        orionGuid = vm.orionGuid;
        hfsVmId = vm.hfsVmId;
    }

    @SuppressWarnings("unchecked")
    private CommandContext setupMockContext() {
        CommandContext mockContext = mock(CommandContext.class);

        when(mockContext.execute(eq("CalculateValidUntil"), any(Function.class), eq(long.class)))
                .thenReturn(validUntil.toEpochMilli());
        when(mockContext.execute(eq("GetVirtualMachine"), any(Function.class), eq(VirtualMachine.class)))
            .thenReturn(vm);
        when(mockContext.execute(eq("CreateVmStopAction"), any(Function.class), eq(long.class)))
                .thenReturn(stopActionId);
        when(mockContext.getId()).thenReturn(UUID.randomUUID());
        return mockContext;
    }

    @Test
    public void calculatesValidUntilWhenAccountCancellationIsProcessed() {
        Instant now = Instant.now();
        Config config = injector.getInstance(Config.class);
        long zombieWaitDuration = Long.parseLong(config.get("vps4.zombie.cleanup.waittime"));
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("CalculateValidUntil"), calculateValidUntilLambdaCaptor.capture(), eq(long.class));

        // Verify that the lambda is returning a date 7 days out
        Function<CommandContext, Long> lambda = calculateValidUntilLambdaCaptor.getValue();
        long calculatedValidUntil = lambda.apply(context);
        Assert.assertEquals(zombieWaitDuration, ChronoUnit.DAYS.between(now, Instant.ofEpochMilli(calculatedValidUntil)));
    }

    @Test
    public void createsStopVmActionWhenAccountCancellationIsProcessed() {
        command.execute(context, request);
        verify(context, times(1))
            .execute(eq("CreateVmStopAction"), createStopVmActionLambdaCaptor.capture(), eq(long.class));

        // Verify that the lambda is creating a stop vm action
        Function<CommandContext, Long> lambda = createStopVmActionLambdaCaptor.getValue();
        long actionId = lambda.apply(context);
        Assert.assertEquals(ActionType.STOP_VM, actionService.getAction(actionId).type);
    }

    @Test
    public void getsHfsVmIdWhenAccountCancellationIsProcessed() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("GetVirtualMachine"), getVirtualMachineLambdaCaptor.capture(), eq(VirtualMachine.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, VirtualMachine> lambda = getVirtualMachineLambdaCaptor.getValue();
        VirtualMachine getVirtualMachine = lambda.apply(context);
        Assert.assertEquals(vm.vmId, getVirtualMachine.vmId);
    }

    @Test
    public void kicksOffStopVmCommandWhenAccountCancellationIsProcessed() {
        command.execute(context, request);
        verify(context, times(1))
            .execute(eq(Vps4StopVm.class), actionRequestArgumentCaptor.capture());

        VmActionRequest request = actionRequestArgumentCaptor.getValue();
        Assert.assertEquals(stopActionId, request.actionId);
        Assert.assertEquals(hfsVmId, request.virtualMachine.hfsVmId);
    }

    @Test
    public void marksVmAsZombieWhenAccountCancellationIsProcessed() {
        Instant before = Instant.now().minusSeconds(60);
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("MarkVmAsZombie"), markZombieLambdaCaptor.capture(), eq(void.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, Void> lambda = markZombieLambdaCaptor.getValue();
        lambda.apply(context);
        Instant setcanceled = vps4VmService.getVirtualMachine(vps4VmId).canceled;
        Instant after = Instant.now().plusSeconds(69);
        Assert.assertTrue(before.isBefore(setcanceled) && after.isAfter(setcanceled));
    }

    @Test
    public void schedulesZombieVmCleanupJobWhenAccountCancellationIsProcessed() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq(ScheduleZombieVmCleanup.class), zombieCleanupArgumentCaptor.capture());

        // Verify the request sent to the ScheduleZombieVmCleanup command
        ScheduleZombieVmCleanup.Request req = zombieCleanupArgumentCaptor.getValue();
        Assert.assertEquals(vps4VmId, req.vmId);
        Assert.assertEquals(validUntil, req.when);
    }

    @Test
    public void recordsJobIdWhenAccountCancellationIsProcessed() {
        UUID retryJobId = UUID.randomUUID();
        when(context.execute(eq(ScheduleZombieVmCleanup.class), any(ScheduleZombieVmCleanup.Request.class)))
                .thenReturn(retryJobId);

        command.execute(context, request);

        verify(context, times(1))
                .execute(eq("RecordScheduledJobId"), eq(Vps4RecordScheduledJobForVm.class), recordJobArgumentCaptor.capture());
        Vps4RecordScheduledJobForVm.Request req = recordJobArgumentCaptor.getValue();
        Assert.assertEquals(retryJobId, req.jobId);
        Assert.assertEquals(ScheduledJob.ScheduledJobType.ZOMBIE, req.jobType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void noOpWhenAnUnclaimedAccountCancellationIsProcessed() {
        virtualMachineCredit.productId = null;
        command.execute(context, request);
        verify(context, times(0)).execute(any(), any(Function.class), any());
        verify(context, times(0)).execute(any(Class.class), any());
    }
}
