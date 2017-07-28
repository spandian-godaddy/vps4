package com.godaddy.vps4.orchestration.phase2;

import com.godaddy.vps4.orchestration.snapshot.WaitForSnapshotAction;
import com.google.inject.*;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.snapshot.SnapshotAction;
import gdg.hfs.vhfs.snapshot.SnapshotService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class WaitForSnapshotActionTest {

    static Injector injector;
    private WaitForSnapshotAction command;
    private CommandContext context;
    private SnapshotAction hfsActionInitial;
    private long hfsActionId = 12345L;

    @Inject SnapshotService hfsSnapshotService;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                }

                @Provides
                public SnapshotService createMockHfsSnapshotService() {
                    return mock(gdg.hfs.vhfs.snapshot.SnapshotService.class);
                }
            }
        );
    }

    @Before
    public void setUpTest() {
        injector.injectMembers(this);
        command = new WaitForSnapshotAction(hfsSnapshotService);
        context = mock(CommandContext.class);
    }

    private void setupMockHfsGetActionCalls(Boolean testSuccess) {
        hfsActionInitial = getHfsAction(SnapshotAction.Status.NEW);
        SnapshotAction inProgressAction = getHfsAction(SnapshotAction.Status.IN_PROGRESS);
        SnapshotAction finalAction = testSuccess
                ? getHfsAction(SnapshotAction.Status.COMPLETE)
                : getHfsAction(SnapshotAction.Status.FAILED);


        when(hfsSnapshotService.getSnapshotAction(eq(hfsActionId)))
                .thenReturn(inProgressAction)
                .thenReturn(inProgressAction)
                .thenReturn(finalAction);
    }

    private SnapshotAction getHfsAction(SnapshotAction.Status status) {
        SnapshotAction action = new SnapshotAction();
        action.actionId = hfsActionId;
        action.status = status;
        return action;
    }

    @Test
    public void queriesActionStatusUntilDone() {
        setupMockHfsGetActionCalls(true);
        SnapshotAction action = command.execute(context, hfsActionInitial);
        verify(hfsSnapshotService, times(3)).getSnapshotAction(eq(hfsActionId));
        Assert.assertEquals(action.actionId, hfsActionId);
    }

    @Test(expected = RuntimeException.class)
    public void raisesExceptionIfActionFails() {
        setupMockHfsGetActionCalls(false);
        command.execute(context, hfsActionInitial);
    }
}
