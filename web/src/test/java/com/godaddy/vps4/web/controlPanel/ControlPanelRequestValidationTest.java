package com.godaddy.vps4.web.controlPanel;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.vm.VmResource;

import com.godaddy.hfs.vm.Vm;

public class ControlPanelRequestValidationTest {

    VirtualMachine vm;
    Image image;

    @Before
    public void setUp() {
        image = Mockito.mock(Image.class);
        image.controlPanel = ControlPanel.PLESK;

        vm = new VirtualMachine(
                UUID.randomUUID(),  // id
                1234,               // vmId
                UUID.randomUUID(),  // orionGuid
                1,                  // projectId
                null,               // ServerSpec
                "fakeName",         // name
                image,              // Image
                null,               // IpAddress
                null,               // validOn
                null,               // canceled                          
                null,               // validUntil
                null,               // nydusAck
                null,               // managedLevelValidOn
                "omg.host.name",    // hostname
                0,                  // managed level
                UUID.randomUUID(),
                null,
                null
        );
    }

    @Test
    public void testCorrectControlPanel() {
        ControlPanelRequestValidation.validateCorrectControlPanel(vm, ControlPanel.PLESK, UUID.randomUUID());
    }

    @Test
    public void testIncorrectControlPanel(){
        try {
            ControlPanelRequestValidation.validateCorrectControlPanel(vm, ControlPanel.CPANEL, UUID.randomUUID());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testGetValidVirtualMachine() {
        VmResource vmResource = Mockito.mock(VmResource.class);
        Mockito.when(vmResource.getVm(Mockito.any(UUID.class))).thenReturn(vm);

        Vm hfsVm = new Vm();
        hfsVm.status = "ACTIVE";
        Mockito.when(vmResource.getVmFromVmVertical(Mockito.anyLong())).thenReturn(hfsVm);

        ControlPanelRequestValidation.getValidVirtualMachine(vmResource, ControlPanel.PLESK, UUID.randomUUID());
    }

}
