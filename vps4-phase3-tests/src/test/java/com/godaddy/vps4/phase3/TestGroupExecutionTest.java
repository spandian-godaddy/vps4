package com.godaddy.vps4.phase3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachinePool;

// @Ignore("Used to test the tests")
public class TestGroupExecutionTest {

    Vps4ApiClient apiClient = mock(Vps4ApiClient.class);

    @Test
    public void testGroupExecution() throws InterruptedException {
        // Mock available credit
        when(apiClient.getVmCredit(any(), any(), any()))
            .thenAnswer(invocation -> UUID.randomUUID());
            //  .thenReturn(null);

        runPhase3Tests();
    }

    @Test(expected=RuntimeException.class)
    public void testNoCreditsAvailable() throws InterruptedException {
        // Test no credit available
        when(apiClient.getVmCredit(any(), any(), any())).thenReturn(null);

        runPhase3Tests();
    }

    public void runPhase3Tests() throws InterruptedException {

        ExecutorService threadPool = Executors.newCachedThreadPool();

        // provisionVm params: String name, UUID orionGuid, String imageName, int dcId, String username, String password
        when(apiClient.provisionVm(any(), any(), any(), anyInt(), any(), any()))
            .thenAnswer(invocation -> {
                JSONObject json = new JSONObject();
                json.put("virtualMachineId", UUID.randomUUID());
                json.put("id", "action-id");
                return json;
            });

        VirtualMachinePool vmPool = new VirtualMachinePool(
                4, 2, 1, apiClient,
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
