package com.godaddy.vps4.orchestration.phase2;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.orchestration.snapshot.WaitForSnapshotAction;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import gdg.hfs.orchestration.CommandContext;
import org.junit.*;

import javax.sql.DataSource;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class Vps4SnapshotVmTest {

    static Injector injector;
    private Vps4SnapshotVm command;
    private CommandContext context;
    private Vps4SnapshotVm.Request request;
    private gdg.hfs.vhfs.snapshot.SnapshotAction hfsAction;
    private gdg.hfs.vhfs.snapshot.Snapshot hfsSnapshot;
    private UUID vps4SnapshotId;
    private long vps4SnapshotActionId;
    private long hfsActionId = 12345L;
    private long hfsSnapshotId = 4567L;
    private String hfsImageId = "nocfoxid";
    private SnapshotService spySnapshotService;

    @Inject Vps4UserService vps4UserService;
    @Inject ProjectService projectService;
    @Inject VirtualMachineService virtualMachineService;
    @Inject SnapshotService snapshotService;
    @Inject gdg.hfs.vhfs.snapshot.SnapshotService hfsSnapshotService;
    @Inject @Named("Snapshot_action") ActionService actionService;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
                new DatabaseModule(),
                new SecurityModule(),
                new SnapshotModule(),
                new Vps4SnapshotTestModule()
        );
    }

    @Before
    public void setUpTest() {
        injector.injectMembers(this);

        spySnapshotService = spy(snapshotService);
        command = new Vps4SnapshotVm(actionService, hfsSnapshotService, spySnapshotService);
        addTestSqlData();
        context = setupMockContext();
        request = getCommandRequest();
    }

    private Vps4SnapshotVm.Request getCommandRequest() {
        Vps4SnapshotVm.Request req = new Vps4SnapshotVm.Request();
        req.actionId = vps4SnapshotActionId;
        req.vps4SnapshotId = vps4SnapshotId;
        req.snapshotName = "test-1";
        return req;
    }

    private CommandContext setupMockContext() {
        CommandContext mockContext = mock(CommandContext.class);
        when(mockContext.getId()).thenReturn(UUID.randomUUID());

        hfsAction = new gdg.hfs.vhfs.snapshot.SnapshotAction();
        hfsAction.actionId = hfsActionId;
        hfsAction.snapshotId = hfsSnapshotId;

        when(mockContext.execute(eq("Vps4SnapshotVm"), any())).thenReturn(hfsAction);
        when(mockContext.execute(eq(WaitForSnapshotAction.class), eq(hfsAction))).thenReturn(hfsAction);

        hfsSnapshot = new gdg.hfs.vhfs.snapshot.Snapshot();
        hfsSnapshot.imageId = hfsImageId;
        when(mockContext.execute(eq("GetHFSSnapshot"), any()))
                .thenReturn(hfsSnapshot);

        return mockContext;
    }

    private void addTestSqlData() {
        SqlTestData.insertUser(vps4UserService);
        Project project = SqlTestData.insertProject(projectService, vps4UserService);
        VirtualMachine vm = SqlTestData.insertVm(virtualMachineService, vps4UserService);
        vps4SnapshotId = SqlTestData.insertSnapshot(snapshotService, vm.vmId, project.getProjectId());
        vps4SnapshotActionId = SqlTestData.insertSnapshotAction(actionService, vps4UserService, vps4SnapshotId);
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(
                injector.getInstance(DataSource.class), injector.getInstance(Vps4UserService.class));
    }

    @Test
    public void kicksOffAHfsRequestToSnapshotVm() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("Vps4SnapshotVm"), any());
    }

    @Test
    public void changesSnapshotStatusToInProgress() {
        command.execute(context, request);
        verify(spySnapshotService, times(1)).markSnapshotInProgress(eq(vps4SnapshotId));
    }

    @Test
    public void updatesTheSnapshotWithTheHfsSnapshotId() {
        command.execute(context, request);
        verify(spySnapshotService, times(1))
                .updateHfsSnapshotId(eq(vps4SnapshotId), eq(hfsSnapshotId));
    }

    @Test
    public void waitsOnTheCompletionOfTheHfsAction() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq(WaitForSnapshotAction.class), eq(hfsAction));
    }

    @Test
    public void marksSnapshotStatusAsComplete() {
        command.execute(context, request);
        verify(spySnapshotService, times(1)).markSnapshotComplete(eq(vps4SnapshotId));
        Assert.assertEquals(snapshotService.getSnapshot(vps4SnapshotId).status, SnapshotStatus.COMPLETE);;
    }

    @Test
    public void queriesHfsForDetailsOfTheCreatedSnapshot() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("GetHFSSnapshot"), any());
    }

    @Test
    public void updatesTheSnapshotWithTheHfsImageId() {
        command.execute(context, request);
        verify(spySnapshotService, times(1))
                .updateHfsImageId(eq(vps4SnapshotId), eq(hfsImageId));
    }

    @Test(expected = RuntimeException.class)
    public void errorInInitialRequestSetsStatusToError() {
        when(context.execute(eq("Vps4SnapshotVm"), any())).thenThrow(new Exception("Error in initial request"));
        command.execute(context, request);

        verify(spySnapshotService, times(1)).markSnapshotErrored(eq(vps4SnapshotId));
        Assert.assertEquals(snapshotService.getSnapshot(vps4SnapshotId).status, SnapshotStatus.ERROR);;
    }

    @Test(expected = RuntimeException.class)
    public void errorInCreationProcessSetsStatusToError() {
        when(context.execute(eq(WaitForSnapshotAction.class), hfsAction))
                .thenThrow(new Exception("Error in initial request"));
        command.execute(context, request);

        verify(spySnapshotService, times(1)).markSnapshotErrored(eq(vps4SnapshotId));
        Assert.assertEquals(snapshotService.getSnapshot(vps4SnapshotId).status, SnapshotStatus.ERROR);;
    }
}
