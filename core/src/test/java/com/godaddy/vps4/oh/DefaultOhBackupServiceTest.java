package com.godaddy.vps4.oh;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.oh.models.OhBackup;
import com.godaddy.vps4.oh.models.OhBackups;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultOhBackupServiceTest {
    @Mock private OhApiBackupService ohApiBackupService1;
    @Mock private OhApiBackupService ohApiBackupService2;
    @Mock private Map<String, OhApiBackupService> ohApiBackupServices;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private VmService vmService;

    @Mock private OhBackups backups;
    @Mock private VirtualMachine vm;
    @Mock private Vm hfsVm;

    private DefaultOhBackupService service;

    @Before
    public void setUp() {
        setUpMockBackups();
        setUpMockVm();
        setUpMockHfsVm();
        when(ohApiBackupServices.get("zone1")).thenReturn(ohApiBackupService1);
        when(ohApiBackupServices.get("zone2")).thenReturn(ohApiBackupService2);
        when(virtualMachineService.getVirtualMachine(vm.vmId)).thenReturn(vm);
        when(vmService.getVm(hfsVm.vmId)).thenReturn(hfsVm);
        when(ohApiBackupService1.getBackups(UUID.fromString(hfsVm.resourceId))).thenReturn(backups);
        when(ohApiBackupService2.getBackups(UUID.fromString(hfsVm.resourceId))).thenReturn(backups);
        service = new DefaultOhBackupService(ohApiBackupServices, virtualMachineService, vmService);
    }

    private void setUpMockBackups() {
        backups.response = mock(OhBackups.Response.class);
        backups.response.data = new ArrayList<>();
        backups.response.data.add(createMockBackup("failed"));
        backups.response.data.add(createMockBackup("complete"));
    }

    private OhBackup createMockBackup(String state) {
        OhBackup backup = mock(OhBackup.class);
        when(backup.getId()).thenReturn(UUID.randomUUID());
        when(backup.getState()).thenReturn(state);
        when(backup.getCreatedAt()).thenReturn(Instant.MIN);
        when(backup.getModifiedAt()).thenReturn(Instant.now());
        return backup;
    }

    private void setUpMockVm() {
        vm.vmId = UUID.fromString("d5500543-8851-48e9-b6d1-4352d969db76");
        vm.hfsVmId = 42522L;
    }

    private void setUpMockHfsVm() {
        hfsVm.vmId = vm.hfsVmId;
        hfsVm.resourceRegion = "zone1";
        hfsVm.resourceId = "22e97fb5-e7f7-41fd-a199-7116350de05d";
    }

    @Test
    public void getBackupsGetsCorrectParameters() {
        service.getBackups(vm.vmId);
        verify(virtualMachineService).getVirtualMachine(vm.vmId);
        verify(vmService).getVm(vm.hfsVmId);
        verify(ohApiBackupServices).get(hfsVm.resourceRegion);
        verify(ohApiBackupService1).getBackups(UUID.fromString(hfsVm.resourceId));
        verify(ohApiBackupService2, never()).getBackups(any(UUID.class));
    }

    @Test
    public void getBackupsCallsCorrectZone() {
        hfsVm.resourceRegion = "zone2";
        service.getBackups(vm.vmId);
        verify(ohApiBackupService1, never()).getBackups(any(UUID.class));
        verify(ohApiBackupService2).getBackups(UUID.fromString(hfsVm.resourceId));
    }

    @Test
    public void getBackupsReturnsBackupList() {
        List<OhBackup> returnValue = service.getBackups(vm.vmId);
        assert backups.response.data == returnValue;
    }

    @Test
    public void suppressesNotFoundException() {
        when(ohApiBackupService1.getBackups(UUID.fromString(hfsVm.resourceId))).thenThrow(new NotFoundException());
        List<OhBackup> returnValue = service.getBackups(vm.vmId);
        assertEquals(0, returnValue.size());
    }
}
