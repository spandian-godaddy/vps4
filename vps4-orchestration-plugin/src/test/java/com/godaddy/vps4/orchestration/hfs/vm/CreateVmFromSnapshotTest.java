package com.godaddy.vps4.orchestration.hfs.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.CreateVMWithFlavorRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.util.Cryptography;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class CreateVmFromSnapshotTest {

    VmService vmService = mock(VmService.class);
    HfsVmTrackingRecordService hfsVmTrackService = mock(HfsVmTrackingRecordService.class);
    Cryptography cryptography = mock(Cryptography.class);
    Injector injector = Guice.createInjector();

    CreateVmFromSnapshot command = new CreateVmFromSnapshot(vmService, cryptography, hfsVmTrackService);
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    CreateVmFromSnapshot.Request request;
    VmAction hfsAction;
    UUID vmId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();

    @Before
    public void setUpTest() {
        request = new CreateVmFromSnapshot.Request();
        request.vmId = vmId;
        request.orionGuid = orionGuid;

        hfsAction = new VmAction();
        hfsAction.vmActionId = 12345;
        hfsAction.vmId = 4567;
        when(vmService.createVmWithFlavor(any(CreateVMWithFlavorRequest.class))).thenReturn(hfsAction);
    }

    @Test
    public void callsHfsVmVerticalToCreateTheVm() {
        command.execute(context, request);
        verify(vmService).createVmWithFlavor(any(CreateVMWithFlavorRequest.class));
    }

    @Test
    public void createsHfsVmTrackingRecord() {
        command.execute(context, request);
        verify(hfsVmTrackService).create(hfsAction.vmId, vmId, orionGuid);
    }

    @Test
    public void commandReturnsHfsVmAction() {
        VmAction result = command.execute(context, request);
        Assert.assertEquals(hfsAction, result);
    }
}
