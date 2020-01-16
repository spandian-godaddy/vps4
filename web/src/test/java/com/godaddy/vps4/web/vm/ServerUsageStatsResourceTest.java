package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.vm.ServerUsageStats;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaGraph;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.panopta.PanoptaUsageGraph;
import com.godaddy.vps4.phase2.CancelActionModule;
import com.godaddy.vps4.phase2.Phase2ExternalsModule;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerUsageStatsService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

@RunWith(MockitoJUnitRunner.class)
public class ServerUsageStatsResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private ServerUsageStatsService serverUsageStatsService = mock(ServerUsageStatsService.class);
    private PanoptaService panoptaService = mock(PanoptaService.class);
    private PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    private ServerUsageStatsResource serverUsageStatsResource, spyResource;
    private VirtualMachine vm;
    private PanoptaDetail panoptaDetail;
    private List<PanoptaGraph> usageGraphs;
    private List<PanoptaGraph> maxUsageGraphs;
    private double cpu;
    private double mem;
    private double disk;

    @Inject
    private VmService vmService;


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
                    bind(PanoptaService.class).toInstance(panoptaService);
                    bind(PanoptaDataService.class).toInstance(panoptaDataService);
                    SchedulerWebService swServ = mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                }
            }
    );

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        serverUsageStatsResource = injector.getInstance(ServerUsageStatsResource.class);
        spyResource = spy(serverUsageStatsResource);
        vm = createTestVm();
        panoptaDetail = new PanoptaDetail(vm.vmId, "partnerCustomerKey",
                                          "customerKey", 42, "serverKey",
                                          Instant.now(), Instant.MAX);
        cpu = Math.random() * 100;
        mem = Math.random() * 100;
        disk = Math.random() * 100;
        setUpUsageGraphs();
        setUpMaxUsageGraphs();
    }

    private void setUpUsageGraphs() {
        usageGraphs = new ArrayList<>();
        usageGraphs.add(setUpUsageGraph(VmMetric.CPU, cpu));
        usageGraphs.add(setUpUsageGraph(VmMetric.RAM, mem));
        usageGraphs.add(setUpUsageGraph(VmMetric.DISK, disk));
    }

    private void setUpMaxUsageGraphs() {
        maxUsageGraphs = new ArrayList<>();
        maxUsageGraphs.add(setUpUsageGraph(VmMetric.CPU, (double) 100));
        maxUsageGraphs.add(setUpUsageGraph(VmMetric.RAM, (double) 100));
        maxUsageGraphs.add(setUpUsageGraph(VmMetric.DISK, (double) 100));
    }

    private PanoptaGraph setUpUsageGraph(VmMetric type, Double value) {
        PanoptaGraph g = new PanoptaUsageGraph();
        g.type = type;
        g.timestamps = new ArrayList<>();
        g.timestamps.add(Instant.now());
        g.values = new ArrayList<>();
        g.values.add(value);
        return g;
    }

    private ServerUsageStats createDummyServerUsageStats() {
        ServerUsageStats serverUsageStats = new ServerUsageStats();
        serverUsageStats.setCpuUsed(0.0);
        serverUsageStats.setMemoryUsed(0);
        serverUsageStats.setMemoryTotal(0);
        serverUsageStats.setDiskUsed(0);
        serverUsageStats.setDiskTotal(0);
        serverUsageStats.setCollected(ZonedDateTime.now());
        serverUsageStats.setRequested(ZonedDateTime.now());
        return serverUsageStats;
    }

    private VirtualMachine createTestVm() {
        vm = new VirtualMachine();
        vm.hfsVmId = 1234;
        vm.vmId = UUID.randomUUID();
        vm.spec = new ServerSpec();
        vm.spec.memoryMib = 4096;
        vm.spec.diskGib = 40;
        return vm;
    }

    @Test
    public void getPanoptaUsage() throws PanoptaServiceException {
        doNothing().when(spyResource).verifyServerIsActive(anyLong());
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        when(panoptaDataService.getPanoptaDetails(vm.vmId)).thenReturn(panoptaDetail);
        when(panoptaService.getUsageGraphs(vm.vmId, "hour")).thenReturn(usageGraphs);

        UsageStats stats = spyResource.getUsage(vm.vmId);

        verify(vmResource, times(1)).getVm(vm.vmId);
        verify(panoptaService, times(1)).getUsageGraphs(vm.vmId, "hour");
        assertNotNull("Stats cannot be null.", stats);
        assertEquals(cpu, stats.cpu.cpuUsagePercent, 0.01);
        assertEquals((long) (vm.spec.memoryMib * mem / 100), stats.mem.memUsed);
        assertEquals(vm.spec.memoryMib, stats.mem.memTotal);
        assertEquals((long) (vm.spec.diskGib * 1024 * disk / 100), stats.disk.diskUsed);
        assertEquals(vm.spec.diskGib * 1024, stats.disk.diskTotal);
    }

    @Test
    public void getPanoptaUsageWithValuesOver100() throws PanoptaServiceException {
        doNothing().when(spyResource).verifyServerIsActive(anyLong());
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        when(panoptaDataService.getPanoptaDetails(vm.vmId)).thenReturn(panoptaDetail);
        when(panoptaService.getUsageGraphs(vm.vmId, "hour")).thenReturn(maxUsageGraphs);

        UsageStats stats = spyResource.getUsage(vm.vmId);

        verify(vmResource, times(1)).getVm(vm.vmId);
        verify(panoptaService, times(1)).getUsageGraphs(vm.vmId, "hour");
        assertNotNull("Stats cannot be null.", stats);
        assertEquals(100, stats.cpu.cpuUsagePercent, 0.01);
        assertEquals(vm.spec.memoryMib, stats.mem.memUsed);
        assertEquals(vm.spec.memoryMib, stats.mem.memTotal);
        assertEquals(vm.spec.diskGib * 1024, stats.disk.diskUsed);
        assertEquals(vm.spec.diskGib * 1024, stats.disk.diskTotal);
    }

    @Test
    public void getPanoptaUsageWithNullLastValue() throws PanoptaServiceException {
        for (PanoptaGraph graph : usageGraphs) {
            graph.timestamps.add(Instant.now());
            graph.values.add(null);
        }

        doNothing().when(spyResource).verifyServerIsActive(anyLong());
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        when(panoptaDataService.getPanoptaDetails(vm.vmId)).thenReturn(panoptaDetail);
        when(panoptaService.getUsageGraphs(vm.vmId, "hour")).thenReturn(usageGraphs);

        UsageStats stats = spyResource.getUsage(vm.vmId);

        verify(vmResource, times(1)).getVm(vm.vmId);
        verify(panoptaService, times(1)).getUsageGraphs(vm.vmId, "hour");
        assertNotNull("Stats cannot be null.", stats);
        assertEquals(cpu, stats.cpu.cpuUsagePercent, 0.01);
        assertEquals((long) (vm.spec.memoryMib * mem / 100), stats.mem.memUsed);
        assertEquals(vm.spec.memoryMib, stats.mem.memTotal);
        assertEquals((long) (vm.spec.diskGib * 1024 * disk / 100), stats.disk.diskUsed);
        assertEquals(vm.spec.diskGib * 1024, stats.disk.diskTotal);
    }

    @Test
    public void getPanoptaUsageWithMultipleNullValues() throws PanoptaServiceException {
        for (PanoptaGraph graph : usageGraphs) {
            graph.timestamps.add(Instant.now());
            graph.timestamps.add(Instant.now());
            graph.values.add(null);
            graph.values.add(null);
        }

        doNothing().when(spyResource).verifyServerIsActive(anyLong());
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        when(panoptaDataService.getPanoptaDetails(vm.vmId)).thenReturn(panoptaDetail);
        when(panoptaService.getUsageGraphs(vm.vmId, "hour")).thenReturn(usageGraphs);

        UsageStats stats = spyResource.getUsage(vm.vmId);

        verify(vmResource, times(1)).getVm(vm.vmId);
        verify(panoptaService, times(1)).getUsageGraphs(vm.vmId, "hour");
        assertNotNull("Stats cannot be null.", stats);
        assertNull(stats.cpu);
        assertNull(stats.mem);
        assertNull(stats.disk);
    }

    @Test(expected = Vps4Exception.class)
    public void getPanoptaUsageInActiveVm() throws PanoptaServiceException {
        Vm inactiveVm = new Vm();
        inactiveVm.status = "STOPPED";

        when(vmResource.getVm(any(UUID.class))).thenReturn(vm);
        when(panoptaDataService.getPanoptaDetails(any(UUID.class))).thenReturn(panoptaDetail);
        when(panoptaService.getUsageGraphs(vm.vmId, "hour")).thenReturn(usageGraphs);
        when(vmService.getVm(vm.hfsVmId)).thenReturn(inactiveVm);

        spyResource.getUsage(UUID.randomUUID());
    }

    @Test(expected = Vps4Exception.class)
    public void getPanoptaUsageNullStats() throws PanoptaServiceException {
        when(vmResource.getVm(any(UUID.class))).thenReturn(vm);
        when(panoptaDataService.getPanoptaDetails(any(UUID.class))).thenReturn(panoptaDetail);
        when(panoptaService.getUsageGraphs(any(UUID.class), anyString())).thenReturn(null);

        spyResource.getUsage(UUID.randomUUID());
    }

    @Test
    public void getHfsUsage() throws PanoptaServiceException {
        ServerUsageStats serverUsageStats = createDummyServerUsageStats();
        doNothing().when(spyResource).verifyServerIsActive(anyLong());
        when(vmResource.getVm(any(UUID.class))).thenReturn(vm);
        when(serverUsageStatsService.getServerUsage(anyLong())).thenReturn(serverUsageStats);

        UsageStats stats = spyResource.getUsage(UUID.randomUUID());

        verify(vmResource, times(1)).getVm(any(UUID.class));
        verify(serverUsageStatsService, times(1)).getServerUsage(anyLong());
        assertTrue("Stats cannot be null.", stats != null);
    }

    @Test(expected = Vps4Exception.class)
    public void getHfsUsageInActiveVm() throws PanoptaServiceException {
        Vm inactiveVm = new Vm();
        inactiveVm.status = "STOPPED";

        ServerUsageStats serverUsageStats = createDummyServerUsageStats();
        when(vmResource.getVm(any(UUID.class))).thenReturn(vm);
        when(serverUsageStatsService.getServerUsage(anyLong())).thenReturn(serverUsageStats);
        when(vmService.getVm(vm.hfsVmId)).thenReturn(inactiveVm);

        spyResource.getUsage(UUID.randomUUID());
    }

    @Test(expected = Vps4Exception.class)
    public void getHfsUsageNullStats() throws PanoptaServiceException {
        when(vmResource.getVm(any(UUID.class))).thenReturn(vm);
        when(serverUsageStatsService.getServerUsage(anyLong())).thenReturn(null);

        spyResource.getUsage(UUID.randomUUID());
    }
}