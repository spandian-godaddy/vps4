package com.godaddy.vps4.phase2.backupstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.backupstorage.jdbc.BackupStorageModel;
import com.godaddy.vps4.backupstorage.BackupStorageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.backupstorage.jdbc.JdbcBackupStorageService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcBackupStorageServiceTest {
    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    private BackupStorageService backupStorageService;
    private VirtualMachine vm;
    private UUID orionGuid = UUID.randomUUID();

    @Before
    public void setUp() throws Exception {
        vm = SqlTestData.insertTestVm(orionGuid, dataSource);
        backupStorageService = new JdbcBackupStorageService(dataSource);
    }

    @After
    public void tearDown() {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
    }

    @Test
    public void canCreateBackup() {
        assertNull(backupStorageService.getBackupStorage(vm.vmId));

        backupStorageService.createBackupStorage(vm.vmId);
        BackupStorageModel storage = backupStorageService.getBackupStorage(vm.vmId);
        assertEquals(vm.vmId, storage.vmId);
        assertNull(storage.ftpServer);
        assertNull(storage.ftpUser);
    }

    @Test
    public void canDestroyBackup() {
        backupStorageService.createBackupStorage(vm.vmId);
        BackupStorageModel storage = backupStorageService.getBackupStorage(vm.vmId);
        assertEquals(vm.vmId, storage.vmId);

        backupStorageService.destroyBackupStorage(vm.vmId);
        assertNull(backupStorageService.getBackupStorage(vm.vmId));
    }

    @Test
    public void canSetBackup() {
        backupStorageService.createBackupStorage(vm.vmId);
        BackupStorageModel storage = backupStorageService.getBackupStorage(vm.vmId);
        assertEquals(vm.vmId, storage.vmId);
        assertNull(storage.ftpServer);
        assertNull(storage.ftpUser);

        backupStorageService.setBackupStorage(vm.vmId, "example.com", "ftp");
        storage = backupStorageService.getBackupStorage(vm.vmId);
        assertEquals("example.com", storage.ftpServer);
        assertEquals("ftp", storage.ftpUser);
    }
}
