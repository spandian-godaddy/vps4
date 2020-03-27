package com.godaddy.vps4.orchestration.backupstorage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.backupstorage.BackupStorageCreds;
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

public class Vps4ResetBackupStorageCredsTest {
    ActionService actionService = mock(ActionService.class);
    BackupStorageCreds creds;
    BackupStorageService backupStorageService = mock(BackupStorageService.class);
    VirtualMachine vm = mock(VirtualMachine.class);
    VmAction hfsAction = mock(VmAction.class);
    VmService vmService = mock(VmService.class);
    WaitForVmAction waitAction = mock(WaitForVmAction.class);

    Vps4ResetBackupStorageCreds command = new Vps4ResetBackupStorageCreds(actionService, backupStorageService, vmService);

    Injector injector = Guice.createInjector(binder -> binder.bind(WaitForVmAction.class).toInstance(waitAction));
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Before
    public void setup() {
        creds = new BackupStorageCreds();
        creds.ftpServer = "ns3160216.ip-151-106-35.eu";
        creds.ftpUser = "ftpback-rbx4-99.ovh.net";
        vm.hfsVmId = 42;
        when(vmService.getBackupStorageCreds(vm.hfsVmId)).thenReturn(creds);
        when(vmService.resetBackupStorageCreds(vm.hfsVmId)).thenReturn(hfsAction);
    }

    @Test
    public void testVps4ResetBackupStorageCreds() {
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);

        verify(vmService, times(1)).resetBackupStorageCreds(request.virtualMachine.hfsVmId);
        verify(context, times(1)).execute(WaitForVmAction.class, hfsAction);
        verify(vmService, times(1)).getBackupStorageCreds(vm.hfsVmId);
        verify(backupStorageService, times(1)).setBackupStorage(vm.vmId, creds.ftpServer, creds.ftpUser);
    }
}
