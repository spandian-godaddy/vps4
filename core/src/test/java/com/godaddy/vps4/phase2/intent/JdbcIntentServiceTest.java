package com.godaddy.vps4.phase2.intent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.godaddy.vps4.intent.IntentService;
import com.godaddy.vps4.intent.jdbc.JdbcIntentService;
import com.godaddy.vps4.intent.model.Intent;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcIntentServiceTest {

    static private Injector injectorForDS;
    private Injector injector;
    static private DataSource dataSource;
    VirtualMachine vm;

    @BeforeClass
    public static void setUpInternalInjector() {
        injectorForDS = Guice.createInjector(new DatabaseModule());
        dataSource = injectorForDS.getInstance(DataSource.class);
    }

    @Before
    public void setUp() {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DataSource.class).toInstance(dataSource);
                bind(IntentService.class).to(JdbcIntentService.class);
            }
        });
    }

    @Test
    public void testGetIntent() {
        List<Intent> intents = injector.getInstance(IntentService.class).getIntents();
        assertNotNull(intents);
    }

    @Test
    public void testSetVmIntents() {
        Vps4User user = SqlTestData.insertTestVps4User(dataSource);
        vm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, user.getId());

        try {
            IntentService intentService = injector.getInstance(IntentService.class);
            Intent intent1 = new Intent();
            intent1.id = 1;
            Intent intent2 = new Intent();
            intent2.id = 10;
            intent2.description = "test description";
            List<Intent> expectedIntents = new ArrayList<>();
            expectedIntents.add(intent1);
            expectedIntents.add(intent2);
            
            List<Intent> actualIntentsFromSet = intentService.setVmIntents(vm.vmId, expectedIntents);

            assertEquals(expectedIntents.size(), actualIntentsFromSet.size());
            assertTrue(actualIntentsFromSet.stream().filter(i -> i.id == intent1.id).findFirst().isPresent());
            assertTrue(actualIntentsFromSet.stream().filter(i -> i.id == intent2.id).findFirst().isPresent());
            assertEquals(intent2.description, actualIntentsFromSet.stream().filter(i -> i.id == intent2.id).findFirst().get().description);
        }
        finally {
            SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
            SqlTestData.deleteTestVps4User(dataSource);
        }
    }

    @Test
    public void testGetVmIntentsEmpty() {
        Vps4User user = SqlTestData.insertTestVps4User(dataSource);
        vm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, user.getId());

        try {
            IntentService intentService = injector.getInstance(IntentService.class);
            List<Intent> actualIntentsFromGet = intentService.getVmIntents(vm.vmId);

            assertNotNull(actualIntentsFromGet);
            assertEquals(0, actualIntentsFromGet.size());
        }
        finally {
            SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
            SqlTestData.deleteTestVps4User(dataSource);
        }
    }
}
