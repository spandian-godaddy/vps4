package com.godaddy.vps4.cpanel;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public class FakeCpanelModuleTest {
    @Test
    public void testProvideAccessHashService() {
        FakeCpanelModule fakeCpanelModule = new FakeCpanelModule();
        CpanelAccessHashService hashService = fakeCpanelModule.provideAccessHashService();
        Random random = new Random();
        long vmId = random.nextLong();
        String publicIp = UUID.randomUUID().toString();
        String fromIp = UUID.randomUUID().toString();
        Instant timeoutAt = Instant.ofEpochSecond(random.nextInt());
        String accessHash = hashService.getAccessHash(vmId, publicIp, fromIp, timeoutAt);

        try {
            Field h = fakeCpanelModule.getClass().getDeclaredField("accessHash");
            h.setAccessible(true);

            Assert.assertEquals(fakeCpanelModule.accessHash, accessHash);
        }
        catch (Exception ex) {
            System.out.println(ex);
            Assert.fail("Failed accessing FakeCpanelModule.accessHash");
        }
    }

    @Test
    public void testInvalidAccessHash() {
        FakeCpanelModule fakeCpanelModule = new FakeCpanelModule();
        CpanelAccessHashService hashService = fakeCpanelModule.provideAccessHashService();
        Random random = new Random();
        long vmId = random.nextLong();
        String accessHash = UUID.randomUUID().toString();
        hashService.invalidAccessHash(vmId, accessHash);
    }
}
