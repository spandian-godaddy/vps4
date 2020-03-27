package com.godaddy.vps4.orchestration.backupstorage;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.backupstorage.BackupStorageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class Vps4CreateBackupStorageTest {
    ActionService actionService = mock(ActionService.class);
    BackupStorageService backupStorageService = mock(BackupStorageService.class);
    VirtualMachine vm = mock(VirtualMachine.class);
    VmAction hfsAction = mock(VmAction.class);
    VmService vmService = mock(VmService.class);
    WaitForVmAction waitAction = mock(WaitForVmAction.class);

    Vps4CreateBackupStorage command = new Vps4CreateBackupStorage(actionService, backupStorageService, vmService);

    Injector injector = Guice.createInjector(binder -> binder.bind(WaitForVmAction.class).toInstance(waitAction));
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Before
    public void setup() {
        vm.hfsVmId = 42;
        when(vmService.createBackupStorage(vm.hfsVmId)).thenReturn(hfsAction);
        doReturn(null).when(context).execute(eq(Vps4ResetBackupStorageCreds.class), anyObject());
    }

    @Test
    public void testVps4CreateBackupStorage() {
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);

        verify(vmService, times(1)).createBackupStorage(request.virtualMachine.hfsVmId);
        verify(context, times(1)).execute(WaitForVmAction.class, hfsAction);
        verify(backupStorageService, times(1)).createBackupStorage(vm.vmId);
        verify(context, times(1)).execute(Vps4ResetBackupStorageCreds.class, request);
    }
}
