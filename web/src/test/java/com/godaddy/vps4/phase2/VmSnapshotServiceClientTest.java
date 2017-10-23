package com.godaddy.vps4.phase2;

import com.godaddy.vps4.client.SsoJwtAuth;
import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.godaddy.vps4.web.client.VmSnapshotService;
import com.godaddy.vps4.web.client.VmSnapshotServiceClientModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import java.util.UUID;

import static org.junit.Assert.fail;


/**
 * This is not a unit test. The test methods in this class actually exercise the code and not just the behaviour.
 * When using an IDE, run the Vps4Application in a separate run configuration.
 * Run this test class as a separate run configuration.
 */
@Ignore
public class VmSnapshotServiceClientTest {

    private Injector injector;
    @Inject @SsoJwtAuth VmSnapshotService vmSnapshotService;

    @Before
    public void setUp() throws Exception {
        injector = Guice.createInjector(
                new ConfigModule(),
                new ObjectMapperModule(),
                new VmSnapshotServiceClientModule()
        );

        injector.injectMembers(this);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getSnapshotsForAVmSucceeds() throws Exception {
        // make sure this vm exists before running this test
        UUID vmId = UUID.fromString("09022e35-6b2d-48c6-b293-d8cdb5207b19");
        Assert.assertEquals(0, vmSnapshotService.getSnapshotsForVM(vmId).size());
    }

    @Test(expected = NotFoundException.class)
    public void getSnapshotsForANonExistentVmFails() throws Exception {
        UUID vmId = UUID.randomUUID();
        vmSnapshotService.getSnapshotsForVM(vmId).size();
    }
}