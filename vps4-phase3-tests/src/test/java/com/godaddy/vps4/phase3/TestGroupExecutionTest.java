package com.godaddy.vps4.phase3;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachinePool;

// @Ignore("Used to test the tests")
public class TestGroupExecutionTest {

    Vps4ApiClient apiClient = mock(Vps4ApiClient.class);

    @Test
    public void testGroupExecution() throws InterruptedException {
        when(apiClient.getVmCredit(any(), any(), any()))
            .thenAnswer(invocation -> UUID.randomUUID());

        runPhase3Tests();
    }

    @Test(expected=RuntimeException.class)
    public void testNoCreditsAvailable() throws InterruptedException {
        // Test no credit available
        when(apiClient.getVmCredit(any(), any(), any())).thenReturn(null);
        runPhase3Tests();
    }

    @Test
    public void testCreditsNotReused() throws InterruptedException {
        when(apiClient.getVmCredit(any(), any(), eq("cpanel")))
            .thenAnswer(invocation -> UUID.randomUUID());
        // Test simulates the api returning a credit that is already in process of being claimed, but not yet marked as claimed
        UUID credit1 = UUID.randomUUID();
        when(apiClient.getVmCredit(any(), any(), eq("MYH")))
            .thenReturn(credit1)
            .thenReturn(credit1)
            .thenReturn(credit1)
            .thenReturn(UUID.randomUUID());
        runPhase3Tests();
        verify(apiClient, times(4)).getVmCredit(any(), any(), eq("MYH"));
        verify(apiClient, times(2)).getVmCredit(any(), any(), eq("cpanel"));
    }

    @Test
    public void testMoreVmsThanCredits() throws InterruptedException {
        // Test tries two vms per image. Limit one credit per image, force vm reuse
        when(apiClient.getVmCredit(any(), any(), eq("MYH")))
            .thenReturn(UUID.randomUUID())
            .thenReturn(null);
        when(apiClient.getVmCredit(any(), any(), eq("cpanel")))
            .thenReturn(UUID.randomUUID())
            .thenReturn(null);
        runPhase3Tests();
    }

    @SuppressWarnings("unchecked")
    public void runPhase3Tests() throws InterruptedException {

        ExecutorService threadPool = Executors.newCachedThreadPool();

        when(apiClient.getListOfExistingVmIds()).thenReturn(new ArrayList<UUID>());

        // provisionVm params: String name, UUID orionGuid, String imageName, int dcId, String username, String password
        when(apiClient.provisionVm(any(), any(), any(), anyInt(), any(), any()))
            .thenAnswer(invocation -> {
                JSONObject json = new JSONObject();
                json.put("virtualMachineId", UUID.randomUUID());
                json.put("id", "123456");
                return json;
            });

        VirtualMachinePool vmPool = new VirtualMachinePool(
                4, 2, 5, apiClient,
                Mockito.mock(Vps4ApiClient.class), "someuser", threadPool);

        TestGroup vps4 = new TestGroup("VPS4 Phase3 Tests");

        List<VmTest> tests = new ArrayList<>();

        VmTest test = new VmTest(){
            @Override
            public String toString() {
                return "one-second-test";
            }
            @Override
            public void execute(VirtualMachine vm) {
                try {
                    Thread.sleep(1000);
                    System.out.println("done with test");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }};

        tests.add(test);
        tests.add(test);

        ImageTestGroup centos7 = new ImageTestGroup("centos-7");
        centos7.addTests(tests);
        vps4.add(centos7);

        ImageTestGroup centos7cPanel = new ImageTestGroup("centos-7-cpanel-11");
        centos7cPanel.addTests(tests);
        vps4.add(centos7cPanel);

        TestGroupExecution testGroupExecution = vps4.execute(threadPool, vmPool);

        testGroupExecution.await();

        threadPool.shutdown();
        while (!threadPool.isTerminated()) {
            threadPool.awaitTermination(2, TimeUnit.SECONDS);
        }

    }
}
