package com.godaddy.vps4.oh.backups;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.godaddy.vps4.oh.OhResponse;
import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.oh.backups.models.OhBackupType;
import com.godaddy.vps4.oh.jobs.OhApiJobService;
import com.godaddy.vps4.oh.jobs.models.OhJob;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultOhBackupServiceTest {
    @Mock private OhApiBackupService ohApiBackupService1;
    @Mock private OhApiBackupService ohApiBackupService2;
    @Mock private Map<String, OhApiBackupService> ohApiBackupServices;
    @Mock private OhApiJobService ohApiJobService;
    @Mock private Map<String, OhApiJobService> ohApiJobServices;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private VmService vmService;

    @Mock private OhResponse<List<OhBackup>> backups;
    @Mock private OhResponse<OhBackup> backup;
    @Mock private OhResponse<OhJob> job;
    @Mock private VirtualMachine vm;
    @Mock private Vm hfsVm;

    private DefaultOhBackupService service;

    @Before
    public void setUp() {
        setUpMockVm();
        setUpMockHfsVm();
        setUpMockBackups();
        when(ohApiBackupServices.get("zone1")).thenReturn(ohApiBackupService1);
        when(ohApiBackupServices.get("zone2")).thenReturn(ohApiBackupService2);
        when(ohApiJobServices.get(anyString())).thenReturn(ohApiJobService);
        when(virtualMachineService.getVirtualMachine(vm.vmId)).thenReturn(vm);
        when(vmService.getVm(hfsVm.vmId)).thenReturn(hfsVm);
        when(ohApiBackupService1.getBackups(eq(UUID.fromString(hfsVm.resourceId)), any(OhBackupState.class)))
                .thenReturn(backups);
        when(ohApiBackupService2.getBackups(eq(UUID.fromString(hfsVm.resourceId)), any(OhBackupState.class)))
                .thenReturn(backups);
        when(ohApiBackupService1.getBackupWithAuthValidation(UUID.fromString(hfsVm.resourceId), backup.value().id))
                .thenReturn(backup);
        when(ohApiBackupService1.getBackup(backup.value().id)).thenReturn(backup);
        when(ohApiJobService.getJob(job.value().id)).thenReturn(job);
        service = new DefaultOhBackupService(ohApiBackupServices, ohApiJobServices, virtualMachineService, vmService);
    }

    private void setUpMockBackups() {
        when(backups.value()).thenReturn(new ArrayList<>());
        backups.value().add(createMockBackup(OhBackupState.FAILED));
        backups.value().add(createMockBackup(OhBackupState.COMPLETE));
        OhBackup backupValue = createMockBackup(OhBackupState.COMPLETE);
        when(backup.value()).thenReturn(backupValue);
        OhJob jobValue = createMockJob(backupValue.jobId);
        when(job.value()).thenReturn(jobValue);
    }

    private OhBackup createMockBackup(OhBackupState status) {
        OhBackup backup = mock(OhBackup.class);
        backup.id = UUID.randomUUID();
        backup.packageId = UUID.fromString(hfsVm.resourceId);
        backup.jobId = UUID.randomUUID();
        return backup;
    }

    private OhJob createMockJob(UUID id) {
        OhJob job = mock(OhJob.class);
        job.id = id;
        return job;
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
        service.getBackups(vm.vmId, OhBackupState.COMPLETE, OhBackupState.PENDING);
        verify(virtualMachineService).getVirtualMachine(vm.vmId);
        verify(vmService).getVm(vm.hfsVmId);
        verify(ohApiBackupServices).get(hfsVm.resourceRegion);
        verify(ohApiBackupService1).getBackups(UUID.fromString(hfsVm.resourceId), OhBackupState.COMPLETE);
        verify(ohApiBackupService1).getBackups(UUID.fromString(hfsVm.resourceId), OhBackupState.PENDING);
        verify(ohApiBackupService2, never()).getBackups(any(UUID.class), eq(OhBackupState.COMPLETE));
        verify(ohApiBackupService2, never()).getBackups(any(UUID.class), eq(OhBackupState.PENDING));
    }

    @Test
    public void getBackupsCallsCorrectZone() {
        hfsVm.resourceRegion = "zone2";
        service.getBackups(vm.vmId, OhBackupState.COMPLETE);
        verify(ohApiBackupService1, never()).getBackups(any(UUID.class), eq(OhBackupState.COMPLETE));
        verify(ohApiBackupService2).getBackups(UUID.fromString(hfsVm.resourceId), OhBackupState.COMPLETE);
    }

    @Test
    public void getBackupsReturnsBackupList() {
        List<OhBackup> returnValue = service.getBackups(vm.vmId, OhBackupState.COMPLETE);
        assertEquals(backups.value().size(), returnValue.size());
        for (int i = 0; i < backups.value().size(); i++) {
            assertEquals(backups.value().get(i), returnValue.get(i));
        }
    }

    @Test
    public void getBackupsSuppressesNotFoundException() {
        when(ohApiBackupService1.getBackups(UUID.fromString(hfsVm.resourceId), OhBackupState.COMPLETE))
                .thenThrow(new NotFoundException());
        List<OhBackup> returnValue = service.getBackups(vm.vmId, OhBackupState.COMPLETE);
        assertEquals(0, returnValue.size());
    }

    @Test
    public void getBackup() {
        OhBackup result = service.getBackup(vm.vmId, backup.value().id);
        verify(virtualMachineService).getVirtualMachine(vm.vmId);
        verify(vmService).getVm(vm.hfsVmId);
        verify(ohApiBackupService1).getBackupWithAuthValidation(UUID.fromString(hfsVm.resourceId), backup.value().id);
        assertSame(backup.value(), result);
    }

    @Test
    public void createBackup() {
        when(ohApiBackupService1.createBackup(UUID.fromString(hfsVm.resourceId), OhBackupType.FULL)).thenReturn(backup);
        OhBackup result = service.createBackup(vm.vmId);
        verify(virtualMachineService).getVirtualMachine(vm.vmId);
        verify(vmService).getVm(vm.hfsVmId);
        verify(ohApiBackupService1).createBackup(UUID.fromString(hfsVm.resourceId), OhBackupType.FULL);
        assertSame(backup.value(), result);
    }

    @Test
    public void deleteBackup() {
        service.deleteBackup(vm.vmId, backup.value().id);
        verify(virtualMachineService).getVirtualMachine(vm.vmId);
        verify(vmService).getVm(vm.hfsVmId);
        verify(ohApiBackupService1).deleteBackup(backup.value().id);
    }

    @Test
    public void restoreBackup() {
        OhJob result = service.restoreBackup(vm.vmId, backup.value().id);
        verify(virtualMachineService).getVirtualMachine(vm.vmId);
        verify(vmService).getVm(vm.hfsVmId);
        verify(ohApiBackupService1).restoreBackup(UUID.fromString(hfsVm.resourceId), backup.value().id, "restore");
        verify(ohApiBackupService1).getBackup(backup.value().id);
        verify(ohApiJobService).getJob(backup.value().jobId);
        assertSame(job.value(), result);
    }
}
