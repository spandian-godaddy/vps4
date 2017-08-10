package com.godaddy.vps4.orchestration.phase2;

import static org.mockito.Mockito.mock;

import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.AbstractModule;

public class Vps4SnapshotTestModule extends AbstractModule {
    @Override
    public void configure() {
        bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
        bind(gdg.hfs.vhfs.snapshot.SnapshotService.class)
                .toInstance(createMockHfsSnapshotService());
    }

    private gdg.hfs.vhfs.snapshot.SnapshotService createMockHfsSnapshotService() {
        return mock(gdg.hfs.vhfs.snapshot.SnapshotService.class);
    }
}
