package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenterService;
import org.junit.Test;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;

public class Vps4PlanChangeTest {
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    CommandContext context = mock(CommandContext.class);
    Vps4PlanChange command = new Vps4PlanChange(virtualMachineService);

    @SuppressWarnings("unchecked")
    @Test
    public void testChangePlanCallsUpdateVmManagedLevel() {
        runChangeManagedLevelToManagedTest();
        verify(context, times(1)).execute(eq("UpdateVmManagedLevel"), any(Function.class), eq(Void.class));
    }

    @SuppressWarnings("unchecked")
    private void runChangeManagedLevelToManagedTest() {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put("managed_level", String.valueOf(2));

        Map<String, String> productMeta = new HashMap<>();
        productMeta.put("product_id", UUID.randomUUID().toString());
        productMeta.put("plan_change_pending", Boolean.FALSE.toString());

        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(UUID.randomUUID().toString())
                .withAccountStatus(AccountStatus.ACTIVE)
                .withShopperID("someShopper")
                .withProductMeta(productMeta)
                .withPlanFeatures(planFeatures)
                .build();
        IpAddress primaryIpAddress = new IpAddress(0, credit.getProductId(), "1.2.3.4", IpAddressType.PRIMARY, 123L, null, null);
        VirtualMachine vm = new VirtualMachine(credit.getProductId(), 1234, credit.getOrionGuid(), 1, null, "testVm", null,
                primaryIpAddress, null, null, null, null, null, 0, null);
        Vps4PlanChange.Request request = new Vps4PlanChange.Request();
        request.vm = vm;
        request.credit = credit;

        when(context.execute(eq("UpdateVmManagedLevel"), any(Function.class), eq(Void.class))).thenReturn(null);

        try {
            command.execute(context, request);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
