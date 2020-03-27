package com.godaddy.hfs.backupstorage;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.backupstorage.BackupStorage;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class BackupStorageTest {
    private Injector injector = Guice.createInjector(new ObjectMapperModule());
    private ObjectMapper mapper;

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        mapper = injector.getInstance(ObjectMapper.class);
    }

    @Test
    public void testDeserializer() throws IOException {
        String json = "{\n" +
                "  \"type\": \"included\",\n" +
                "  \"readOnlyDate\": null,\n" +
                "  \"status\": \"DEACTIVATING\",\n" +
                "  \"id\": 3,\n" +
                "  \"quota\": {\n" +
                "    \"unit\": \"GB\",\n" +
                "    \"value\": 500\n" +
                "  },\n" +
                "  \"usage\": {\n" +
                "    \"unit\": \"GB\",\n" +
                "    \"value\": 20\n" +
                "  },\n" +
                "  \"ftpBackupName\": \"ftpback-rbx2-139.ovh.net\",\n" +
                "  \"vm_id\": 24165\n" +
                "}";
        BackupStorage backupStorage = mapper.readValue(json, BackupStorage.class);
        assertEquals(BackupStorage.Status.DEACTIVATING, backupStorage.status);
        assertEquals(20480, backupStorage.diskUsed());
        assertEquals(512000, backupStorage.diskTotal());
    }

    @Test
    public void testNullValues() throws IOException {
        String json = "{\n" +
                "  \"type\": \"included\",\n" +
                "  \"readOnlyDate\": null,\n" +
                "  \"status\": \"DEACTIVATING\",\n" +
                "  \"id\": 3,\n" +
                "  \"quota\": {\n" +
                "    \"unit\": \"GB\",\n" +
                "    \"value\": 500\n" +
                "  },\n" +
                "  \"usage\": {\n" +
                "    \"unit\": null,\n" +
                "    \"value\": null\n" +
                "  },\n" +
                "  \"ftpBackupName\": \"ftpback-rbx2-139.ovh.net\",\n" +
                "  \"vm_id\": 24165\n" +
                "}";
        BackupStorage backupStorage = mapper.readValue(json, BackupStorage.class);
        assertEquals(0, backupStorage.diskUsed());
        assertEquals(512000, backupStorage.diskTotal());
    }

    @Test
    public void testPercentages() throws IOException {
        String json = "{\n" +
                "  \"type\": \"included\",\n" +
                "  \"readOnlyDate\": null,\n" +
                "  \"status\": \"ACTIVATED\",\n" +
                "  \"id\": 3,\n" +
                "  \"quota\": {\n" +
                "    \"unit\": \"TB\",\n" +
                "    \"value\": 3\n" +
                "  },\n" +
                "  \"usage\": {\n" +
                "    \"unit\": \"%\",\n" +
                "    \"value\": 50\n" +
                "  },\n" +
                "  \"ftpBackupName\": \"ftpback-rbx2-139.ovh.net\",\n" +
                "  \"vm_id\": 24165\n" +
                "}";
        BackupStorage backupStorage = mapper.readValue(json, BackupStorage.class);
        assertEquals(1572864, backupStorage.diskUsed());
        assertEquals(3145728, backupStorage.diskTotal());
    }
}
