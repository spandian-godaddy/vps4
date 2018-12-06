package com.godaddy.vps4.phase2;


import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.SnapshotScheduleResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class SnapshotScheduleResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    private GDUser user;
    private VirtualMachine testVm;

    private SchedulerWebService schedulerWebService = mock(SchedulerWebService.class);

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(SchedulerWebService.class).toInstance(schedulerWebService);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });


    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private void createTestVm() {
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
        testVm = SqlTestData.insertTestVm(UUID.randomUUID(), vps4User.getId(), dataSource);
    }

    private SnapshotScheduleResource getSnapshotScheduleResource() {
        return injector.getInstance(SnapshotScheduleResource.class);
    }


    @Test
    public void testPauseCallsScheduler() {
        createTestVm();
        getSnapshotScheduleResource().pauseAutomaicSnapshots(testVm.vmId);
        verify(schedulerWebService, times(1)).pauseJob("vps4", "backups", testVm.backupJobId);
    }

    @Test
    public void testResumeCallsScheduler() {
        createTestVm();
        getSnapshotScheduleResource().resumeAutomaticSnapshots(testVm.vmId);
        verify(schedulerWebService, times(1)).resumeJob("vps4", "backups", testVm.backupJobId);
    }
}
