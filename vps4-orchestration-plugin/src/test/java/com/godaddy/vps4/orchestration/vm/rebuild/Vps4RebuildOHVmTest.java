package com.godaddy.vps4.orchestration.vm.rebuild;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.hfs.vm.RebuildVm;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;

public class Vps4RebuildOHVmTest extends Vps4RebuildVmTest {

    @Override
    @Before
    public void setupTest() {
        newHfsVmId = originalHfsVmId;

        super.setupTest();

        request.rebuildVmInfo.image.hfsName = "hfs-centos70-cpanel-11-x86_64-vmtempl";
        doReturn(action).when(context).execute(eq("RebuildOHVm"), eq(RebuildVm.class), any());
        command = new Vps4RebuildOHVm(actionService, virtualMachineService, vps4NetworkService, vmUserService,
                                      creditService, panoptaDataService, hfsVmTrackingRecordService);
    }

    @Override
    @Test
    public void getsPublicIpAddresses() {
        command.execute(context, request);
        verify(vps4NetworkService, never()).getVmIpAddresses(vps4VmId);
    }

    @Override
    @Test
    public void unbindsPublicIps() {
        command.execute(context, request);
        verify(context, never()).execute(startsWith("UnbindIP-"), eq(UnbindIp.class), anyObject());
    }

    @Override
    @Test
    public void deletesOriginalVm() {
        command.execute(context, request);
        verify(context, never()).execute(eq("DestroyVmHfs"), eq(DestroyVm.class), anyObject());
    }

    @Override
    @Test
    public void createsNewVm() {
        command.execute(context, request);
        verify(context, never()).execute(eq("CreateVm"), eq(CreateVm.class), anyObject());
    }

    @Override
    @Test
    public void bindsPublicIps() {
        command.execute(context, request);
        verify(context, never()).execute(startsWith("BindIP-"), eq(BindIp.class), anyObject());
    }

    @Test
    public void rebuildsVmInHfs() {
        command.execute(context, request);
        ArgumentCaptor<RebuildVm.Request> argument = ArgumentCaptor.forClass(RebuildVm.Request.class);
        verify(context).execute(eq("RebuildOHVm"), eq(RebuildVm.class), argument.capture());
        RebuildVm.Request request = argument.getValue();
        assertEquals(originalHfsVmId, request.vmId);
        assertEquals("host.name", request.hostname);
        assertEquals("hfs-centos70-cpanel-11-x86_64-vmtempl", request.image_name);
        assertEquals("user", request.username);
    }

    @Test
    public void updatesVmRecordOnCreate() {
        command.execute(context, request);
        verify(virtualMachineService, never()).addHfsVmIdToVirtualMachine(vps4VmId, action.vmId);

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put("name", "server-name");
        expectedParams.put("image_id", 7L);
        verify(virtualMachineService).updateVirtualMachine(vps4VmId, expectedParams);
    }

    @Test
    public void waitsForAndRecordsVmAction() {
        command.execute(context, request);
        verify(context).execute(WaitForAndRecordVmAction.class, action);
    }

    @Test
    public void updateHfsVmTrackingRecord() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("UpdateHfsVmTrackingRecord"), any(Function.class), eq(Void.class));
    }
}
