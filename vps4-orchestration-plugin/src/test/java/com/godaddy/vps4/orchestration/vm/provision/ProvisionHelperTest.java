package com.godaddy.vps4.orchestration.vm.provision;

import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

public class ProvisionHelperTest {

    @Test
    public void createSetRootPasswordRequest() {
        long hfsVmId = 123L;
        String password = "foobar";
        String controlPanel = "myh";
        SetPassword.Request req = ProvisionHelper.createSetRootPasswordRequest(hfsVmId, password.getBytes(), controlPanel);
        assertEquals(req.hfsVmId, hfsVmId);
        assertEquals(req.controlPanel, controlPanel);
        assertThat(Arrays.asList(password.getBytes()), containsInAnyOrder(req.encryptedPassword));
        assertThat(Arrays.asList("root"), containsInAnyOrder(req.usernames.toArray()));
    }

}