package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.vm.ServerUsageStats;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.CancelActionModule;
import com.godaddy.vps4.phase2.Phase2ExternalsModule;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

@RunWith(MockitoJUnitRunner.class)
public class ServerUsageStatsResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private ServerUsageStatsService serverUsageStatsService = mock(ServerUsageStatsService.class);
    private ServerUsageStatsResource serverUsageStatsResource;
    private VirtualMachine vm;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(ServerUsageStatsService.class).toInstance(serverUsageStatsService);
                    bind(VmResource.class).toInstance(vmResource);
                    SchedulerWebService swServ = mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                }
            }
    );

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        serverUsageStatsResource = injector.getInstance(ServerUsageStatsResource.class);
        vm = createTestVm();
    }

    private ServerUsageStats createDummyServerUsageStats() {
        ServerUsageStats serverUsageStats = new ServerUsageStats();
        serverUsageStats.setCpuUsed(0.0);
        serverUsageStats.setMemoryUsed(0);
        serverUsageStats.setMemoryTotal(0);
        serverUsageStats.setDiskUsed(0);
        serverUsageStats.setDiskTotal(0);
        serverUsageStats.setCollected(ZonedDateTime.now());
        return serverUsageStats;
    }

    private VirtualMachine createTestVm() {
        vm = new VirtualMachine();
        vm.hfsVmId = 1234;
        vm.vmId = UUID.randomUUID();
        return vm;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getsServerUsage() {
        ServerUsageStats serverUsageStats = createDummyServerUsageStats();
        when(vmResource.getVm(any(UUID.class))).thenReturn(vm);
        when(serverUsageStatsService.getServerUsage(anyLong())).thenReturn(serverUsageStats);
        when(vmResource.getVm(any(UUID.class))).thenReturn(vm);

        UsageStats stats = serverUsageStatsResource.getUsage(UUID.randomUUID());

        verify(vmResource, times(1)).getVm(any(UUID.class));
        verify(serverUsageStatsService, times(1)).getServerUsage(anyLong());
        assertTrue("Stats cannot be null.", stats!= null);
    }
}