package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineType;
import com.godaddy.vps4.web.security.GDUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetVirtualMachineTest {

    private UUID vmId = UUID.randomUUID();
    private UUID vmIdOtherDc = UUID.randomUUID();
    private VmResource vmResource;
    private DataCenter dc;
    private Vm hfsVm;
    private long hfsVmId = 1234;
    private UUID orionGuid = UUID.randomUUID();
    private VirtualMachine vm, vmOtherDc;
    private GDUser user;

    @Before
    public void setupTest() {
        dc = new DataCenter(5, "testDc");
        DataCenterService dcService = mock(DataCenterService.class);
        when(dcService.getDataCenter(dc.dataCenterId)).thenReturn(dc);

        Vps4UserService userService = getMockedUserService();
        VmService vmService = getMockedVmService();
        VirtualMachineService virtualMachineService = getMockedVirtualMachineService();
        CreditService creditService = getMockedCreditService(dcService);
        Config config = getMockedConfig();
        Cryptography cryptography = mock(Cryptography.class);
        DataCenterService dataCenterService = mock(DataCenterService.class);

        vmResource = new VmResource(user, vmService, userService, virtualMachineService, creditService, null, null, null,
                                    config, cryptography, dcService, null, dataCenterService);
    }

    private Config getMockedConfig() {
        Config config = mock(Config.class);
        when(config.get(Mockito.anyString(), Mockito.anyString())).thenReturn("0");
        when(config.get(Mockito.anyString())).thenReturn("0");
        when(config.getData(Mockito.anyString())).thenReturn("cxPTMJetZeRW5ofrsUp0wecvNKsjf1/NHwllp0JllBM=".getBytes());
        return config;
    }

    private CreditService getMockedCreditService(DataCenterService dataCenterService) {
        Map<String, String> productMeta = new HashMap<>();
        productMeta.put("product_id", vmId.toString());
        productMeta.put("data_center", String.valueOf(dc.dataCenterId));

        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(dataCenterService)
                .withAccountGuid(vm.orionGuid.toString())
                .withShopperID(user.getShopperId())
                .withAccountStatus(AccountStatus.ACTIVE)
                .withProductMeta(productMeta)
                .build();

        CreditService creditService = mock(CreditService.class);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);
        return creditService;
    }

    private VirtualMachineService getMockedVirtualMachineService() {
        IpAddress ipAddress = new IpAddress();
        ipAddress.ipAddressType = IpAddressType.PRIMARY;
        ipAddress.ipAddress = "127.0.0.1";
        ipAddress.hfsAddressId = 1;
        VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
        vm = new VirtualMachine(vmId,
                                hfsVmId,
                                orionGuid,
                                1,
                                null,
                                "Unit Test Vm",
                                null,
                                ipAddress,
                                Instant.now(),
                                Instant.now().plus(24, ChronoUnit.HOURS),
                                Instant.now().plus(24, ChronoUnit.HOURS),
                                Instant.now(),
                                null,
                                0,
                                UUID.randomUUID(),
                                null);
        vmOtherDc = new VirtualMachine(vmIdOtherDc,
                3L,
                UUID.randomUUID(),
                2,
                null,
                "Unit Test Vm 2",
                null,
                ipAddress,
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS),
                Instant.now().plus(24, ChronoUnit.HOURS),
                Instant.now(),
                null,
                0,
                UUID.randomUUID(),
                null);

        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);
        when(virtualMachineService
                .getVirtualMachines(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), eq(null)))
                .thenReturn(Collections.singletonList(vm));
        when(virtualMachineService
                .getVirtualMachines(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), eq(2)))
                .thenReturn(Collections.singletonList(vmOtherDc));
        return virtualMachineService;
    }

    private VmService getMockedVmService() {
        hfsVm = new Vm();
        hfsVm.vmId = hfsVmId;
        hfsVm.resourceId = "foobar";
        VmService vmService = mock(VmService.class);
        when(vmService.getVm(hfsVmId)).thenReturn(hfsVm);
        return vmService;
    }

    private Vps4UserService getMockedUserService() {
        user = GDUserMock.createEmployee2Shopper("testshopper");
        Vps4UserService userService = mock(Vps4UserService.class);
        Vps4User vps4User = mock(Vps4User.class);
        when(vps4User.getId()).thenReturn(1L);
        when(userService.getOrCreateUserForShopper(user.getShopperId(), "1", UUID.randomUUID())).thenReturn(vps4User);
        when(userService.getUser(user.getShopperId())).thenReturn(vps4User);
        return userService;
    }

    @Test
    public void testGetVirtualMachinesForShopperId() {
        List<VirtualMachine> vms =
                vmResource.getVirtualMachines(VirtualMachineType.ACTIVE, user.getShopperId(), null, null, null, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetZombieVirtualMachinesForShopperId() {
        List<VirtualMachine> vms =
                vmResource.getVirtualMachines(VirtualMachineType.ZOMBIE, user.getShopperId(), null, null, null, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByIpAddress() {
        List<VirtualMachine> vms = vmResource.getVirtualMachines(null, null, "127.0.0.1", null, null, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByOrionGuid() {
        List<VirtualMachine> vms = vmResource.getVirtualMachines(null, null, null, orionGuid, null, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByHfsVmId() {
        List<VirtualMachine> vms = vmResource.getVirtualMachines(null, null, null, null, hfsVmId, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByShopperId() {
        List<VirtualMachine> vms = vmResource.getVirtualMachines(null, user.getShopperId(), null, null, null, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByDcId() {
        List<VirtualMachine> vms = vmResource.getVirtualMachines(null, null, null, null, null, 2);
        assertEquals(1, vms.size());
        assertEquals(vmIdOtherDc, vms.get(0).vmId);
    }
}
