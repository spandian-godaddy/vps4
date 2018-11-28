package com.godaddy.vps4.orchestration.phase2;

import com.google.inject.AbstractModule;
import gdg.hfs.vhfs.snapshot.SnapshotService;
import com.godaddy.hfs.vm.VmService;

import static org.mockito.Mockito.mock;

public class Vps4ExternalsModule extends AbstractModule {
    @Override
    public void configure() {
        bind(SnapshotService.class).toInstance(createMockHfsSnapshotService());
        bind(VmService.class).toInstance(createMockHfsVmService());
    }

    private SnapshotService createMockHfsSnapshotService() {
        return mock(SnapshotService.class);
    }

    private VmService createMockHfsVmService() {
        return mock(VmService.class);
    }
}
