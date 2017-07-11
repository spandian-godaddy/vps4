package com.godaddy.vps4.orchestration.phase2;

import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import static org.mockito.Mockito.mock;

/**
 * Created by ashetty on 7/26/17.
 */
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
