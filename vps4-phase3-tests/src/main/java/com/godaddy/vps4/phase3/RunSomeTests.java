package com.godaddy.vps4.phase3;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.tests.ChangeHostnameTest;
import com.godaddy.vps4.phase3.tests.StopStartVmTest;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachinePool;


/*
 * arg[0] = the url that you plan to run against.  Example: http://127.0.0.1:8089/
 * arg[1] = the authorization key to log in to the url.  Example: "sso-jwt afJhbGciOiAiUlMyNTYiLCAia2lkIjogInhqQzVZZE1BZVEifQ.eyJhY2NvdW50TmFtZSI6ICJnc2hlcHBhcmQiLCAiYXV0aCI6ICJiYXNpYyIsICJmYWN0b3JzIjogeyJrX3B3IjogMTQ5MTMzMjI3MX0sICJmaXJzdG5hbWUiOiAiR2VvcmdlIiwgImZ0YyI6IDEsICJncm91cHMiOiBbIkRldi1WZXJ0aWdvIiwgIlRvb2x6aWxsYS1RQSIsICJRQSJdLCAiaWF0IjogMTQ5MTMzMjI3MSwgImp0aSI6ICJOUmM1YnhlT2FqZmliNkdMRU5qNmlBIiwgImxhc3RuYW1lIjogIlNoZXBwYXJkIiwgInR5cCI6ICJqb21heCJ9.vddEc26-NeFWvXGIYkelLdpr8qOHwatGKy5uHNYUMbsfPw3CTclCRNbBn0Ixy0mDjfZUFooHY4DWiP_cR24u8sV7mdnvAW6osnGJcfpnNUYzWEOntOjSI_IpipNXNtXYvCVEsJcGiSzs3nBMAqTHLQuwqydCJn65JLaRLkrFrXY"
 * */

public class RunSomeTests {

    static String USERNAME = "testuser";
    static String PASSWORD = "testVPS4YOU!";

    public static void main(String[] args) throws Exception{

        String URL = args[0];
        String user = args[1];
        String authHeader = args[2];

        Vps4ApiClient vps4ApiClient = new Vps4ApiClient(URL, authHeader);

        VirtualMachinePool vmPool = new VirtualMachinePool(4, 2, vps4ApiClient, user);

        ExecutorService threadPool = Executors.newCachedThreadPool();

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
