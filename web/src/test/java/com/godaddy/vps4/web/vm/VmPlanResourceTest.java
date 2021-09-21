package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.plan.Plan;
import com.godaddy.vps4.plan.PlanService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.VirtualMachine;

@RunWith(MockitoJUnitRunner.class)
public class VmPlanResourceTest {
    private VmPlanResource vmPlanResource;

    @Mock private VmResource vmResource;
    @Mock private PlanService planService;

    @Mock private Action action;
    @Mock private VirtualMachine vm;

    @Before
    public void setup() {
        vm.image = mock(Image.class);
        vm.image.operatingSystem = Image.OperatingSystem.WINDOWS;
        vm.vmId = UUID.randomUUID();
        action.id = new Random().nextLong();
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        vmPlanResource = new VmPlanResource(vmResource, planService);
    }

    @Test
    public void callsGetVmForAuthValidation() {
        vmPlanResource.getPlansForVm(vm.vmId);
        verify(vmResource, times(1)).getVm(vm.vmId);
    }

    @Test
    public void getsAdjacentPlanList() {
        List<Plan> expectedList = new ArrayList<>();
        when(planService.getAdjacentPlanList(vm)).thenReturn(expectedList);
        List<Plan> actualList = vmPlanResource.getPlansForVm(vm.vmId);
        verify(planService, times(1)).getAdjacentPlanList(vm);
        assertEquals(expectedList, actualList);
    }
}
