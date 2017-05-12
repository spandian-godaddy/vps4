package com.godaddy.vps4.phase3;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.api.SsoClient;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.tests.ChangeHostnameTest;
import com.godaddy.vps4.phase3.tests.StopStartVmTest;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachinePool;


/*
 * arg[0] = the vps4 url that you plan to run against.  Example: http://127.0.0.1:8089/
 * arg[1] = the vps4 shopperId
 * arg[2] = the vps4 password
 * arg[3] = the SSO URL to get the admin token from: (example: https://sso.dev-godaddy.com/v1/api/token)
 * arg[4] = the admin (jomax) username
 * arg[5] = the admin password
 * */

public class RunSomeTests {

    private static final Logger logger = LoggerFactory.getLogger(RunSomeTests.class);

    static String USERNAME = "testuser";
    static String PASSWORD = "testVPS4YOU!";

    static final int MAX_TOTAL_VM = 2;
    static final int MAX_PER_IMAGE_VM = 1;

    public static void main(String[] args) throws Exception{

        String URL = args[0];
        String vps4ShopperId = args[1];
        String vps4Password = args[2];

        String ssoUrl = args[3];
        String adminUser = args[4];
        String adminPassword = args[5];

        for (int i=0; i < args.length; i++)
            logger.error("Arg {}: {}", i, args[i]);

        SsoClient ssoClient = new SsoClient(ssoUrl);

        String adminAuthHeader = ssoClient.getJomaxSsoToken(adminUser, adminPassword);
        Vps4ApiClient adminClient = new Vps4ApiClient(URL, adminAuthHeader);

        String vps4AuthHeader = ssoClient.getVps4SsoToken(vps4ShopperId, vps4Password);
        Vps4ApiClient vps4ApiClient = new Vps4ApiClient(URL, vps4AuthHeader);

        ExecutorService threadPool = Executors.newCachedThreadPool();

        VirtualMachinePool vmPool = new VirtualMachinePool(MAX_TOTAL_VM, MAX_PER_IMAGE_VM, vps4ApiClient,
                adminClient, vps4ShopperId, threadPool);

        List<VmTest> tests = Arrays.asList(
                new ChangeHostnameTest(randomHostname()),
                new StopStartVmTest()
        );

        TestGroup vps4 = new TestGroup("VPS4 Phase3 Tests");

        ImageTestGroup centos7 = new ImageTestGroup("centos-7");
        centos7.addTests(tests);
        vps4.add(centos7);

        ImageTestGroup centos7cPanel = new ImageTestGroup("centos-7-cPanel-11");
        centos7cPanel.addTests(tests);
        vps4.add(centos7cPanel);

        TestGroupExecution testGroupExecution = vps4.execute(threadPool, vmPool);

        try{
            testGroupExecution.await();
        } catch(Exception e){
            testGroupExecution.status = TestStatus.FAIL;
        }

        threadPool.shutdown();

        try{
            threadPool.awaitTermination(1, TimeUnit.HOURS);
        } catch(InterruptedException e) {
            System.err.println("Interrupted waiting for test termination");
            vmPool.destroyAll();
            printResults(testGroupExecution);
            System.exit(1);
        }

        vmPool.destroyAll();
        printResults(testGroupExecution);

    }

    private static void printResults(TestGroupExecution testGroupExecution){
        System.out.println("===========================================");
        System.out.println("===========================================");
        testGroupExecution.printResults();
        System.out.println("===========================================");
        if (testGroupExecution.status != TestStatus.PASS){
            System.err.println("Non-Passing test found.  Exiting with status = 1");
            System.exit(1);
        }
    }

    public static String randomHostname(){
        String AB = "abcdefghijklmnopqrstuvwxyz";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for(int x = 0; x<3; x++)
        {
          for( int i = 0; i < 5; i++ )
             sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
          if(x<2){
              sb.append(".");
          }
        }
        return sb.toString();
     }

}
