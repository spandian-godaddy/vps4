package com.godaddy.vps4.orchestration.ohbackup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.oh.OhBackupDataService;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class Vps4DestroyOhBackupTest {
    @Mock ActionService actionService;
    @Mock CommandContext commandContext;
    @Mock OhBackupService ohBackupService;
    @Mock OhBackupDataService ohBackupDataService;

    private Vps4DestroyOhBackup command;
    private Vps4DestroyOhBackup.Request request;

    @Before
    public void setUp() {
        setUpRequest();
        command = new Vps4DestroyOhBackup(actionService, ohBackupService, ohBackupDataService);
    }

    private void setUpRequest() {
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.vmId = UUID.randomUUID();
        request = new Vps4DestroyOhBackup.Request(vm, UUID.randomUUID());
    }

    @Test
    public void callsBackupService() {
        command.executeWithAction(commandContext, request);
        verify(ohBackupService).deleteBackup(request.virtualMachine.vmId, request.backupId);
    }

    @Test
    public void callsBackupDataService() {
        command.executeWithAction(commandContext, request);
        verify(ohBackupDataService).destroyBackup(request.backupId);
    }
}
