package com.godaddy.vps4.phase3;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.simple.JSONObject;
import org.junit.Test;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.tests.SayMessageTest;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachinePool;

import ch.qos.logback.classic.Level;

public class TestGroupExecutionTest {

    static {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.ALL);
    }

    @Test
    public void testExecute() throws Exception {

        Vps4ApiClient vps4ApiClient = new Vps4ApiClient(null, null) {

            @Override
            protected Result callApi(HttpClient client, HttpRequestBase request) {

                JSONObject json = new JSONObject();
                json.put("id", "42");
                json.put("status", "COMPLETE");
                json.put("orionGuid", UUID.randomUUID().toString());
                json.put("virtualMachineId", UUID.randomUUID().toString());

                return new Result(200, new StringBuffer(json.toJSONString()));
            }

        };

        VirtualMachinePool vmPool = new VirtualMachinePool(10, 5, vps4ApiClient);

        ExecutorService threadPool = Executors.newFixedThreadPool(2);

        List<VmTest> tests = Arrays.asList(
            new SayMessageTest("Hello!")
        );

        TestGroup vps4 = new TestGroup("VPS4 Phase3 Tests");

        ImageTestGroup centos7 = new ImageTestGroup("centos-7");
        centos7.addTests(tests);
        vps4.add(centos7);

        ImageTestGroup centos7cPanel = new ImageTestGroup("centos-7-cPanel-11");
        centos7cPanel.addTests(tests);
        vps4.add(centos7cPanel);

        TestGroupExecution testGroupExecution = vps4.execute(threadPool, vmPool);

        testGroupExecution.await();

        System.out.println("execution: " + testGroupExecution);

        StringWriter writer = new StringWriter();
        new TestGroupOutputWriter(writer).write(testGroupExecution);
        System.out.println(writer.toString());

        threadPool.shutdown();

        threadPool.awaitTermination(5, TimeUnit.SECONDS);

    }
}
