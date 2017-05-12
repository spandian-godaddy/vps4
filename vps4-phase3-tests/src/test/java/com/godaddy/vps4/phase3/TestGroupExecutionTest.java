package com.godaddy.vps4.phase3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachinePool;

public class TestGroupExecutionTest {

    @Test
    public void testGroupExecution() throws InterruptedException {

        ExecutorService threadPool = Executors.newCachedThreadPool();

        Vps4ApiClient apiClient = mock(Vps4ApiClient.class);

        System.out.println(apiClient);

        when(apiClient.getVmCredit(any(), any(), any()))
            .thenAnswer(invocation -> UUID.randomUUID());


        when(apiClient.deleteVm(any(UUID.class))).thenReturn(null);

        // String name, UUID orionGuid, String imageName, int dcId, String username, String password

        when(apiClient.provisionVm(any(String.class), any(UUID.class), any(String.class), anyInt(), any(String.class), any(String.class)))
            .thenAnswer(invocation -> {
                JSONObject json = new JSONObject();
                json.put("virtualMachineId", UUID.randomUUID());
                return json;
            });

        VirtualMachinePool vmPool = new VirtualMachinePool(
                20, 1, apiClient,
                Mockito.mock(Vps4ApiClient.class), "someuser", threadPool);

        TestGroup vps4 = new TestGroup("VPS4 Phase3 Tests");

        List<VmTest> tests = new ArrayList<>();

        VmTest test = vm -> {
            try {
                Thread.sleep(3000);
                System.out.println("done with test");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        tests.add(test);
        tests.add(test);
        tests.add(test);
        tests.add(test);

        ImageTestGroup centos7 = new ImageTestGroup("centos-7");
        centos7.addTests(tests);
        vps4.add(centos7);

        ImageTestGroup centos7cPanel = new ImageTestGroup("centos-7-cPanel-11");
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
