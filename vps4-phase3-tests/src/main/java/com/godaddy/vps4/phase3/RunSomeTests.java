package com.godaddy.vps4.phase3;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.api.SsoClient;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.tests.ChangeHostnameTest;
import com.godaddy.vps4.phase3.tests.StopStartVmTest;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachinePool;

public class RunSomeTests {

    private static final Logger logger = LoggerFactory.getLogger(RunSomeTests.class);

    public static void main(String[] args) throws Exception{

        CommandLine cmd = parseCliArgs(args);
        String URL = cmd.getOptionValue("api-url");
        String vps4ShopperId = cmd.getOptionValue("shopper");
        String vps4Password = cmd.getOptionValue("password");

        String ssoUrl = cmd.getOptionValue("sso-url");
        String adminUser = cmd.getOptionValue("admin");
        String adminPassword = cmd.getOptionValue("admin-pass");

        int maxTotalVm = Integer.parseInt(cmd.getOptionValue("max-vms"));
        int maxPerImageVm = Integer.parseInt(cmd.getOptionValue("pool-size"));
        int maxVmWaitSeconds = Integer.parseInt(cmd.getOptionValue("vm-timeout"));

        SsoClient ssoClient = new SsoClient(ssoUrl);

        String adminAuthHeader = ssoClient.getJomaxSsoToken(adminUser, adminPassword);
        Vps4ApiClient adminClient = new Vps4ApiClient(URL, adminAuthHeader);

        String vps4AuthHeader = ssoClient.getVps4SsoToken(vps4ShopperId, vps4Password);
        Vps4ApiClient vps4ApiClient = new Vps4ApiClient(URL, vps4AuthHeader);

        ExecutorService threadPool = Executors.newCachedThreadPool();

        VirtualMachinePool vmPool = new VirtualMachinePool(maxTotalVm, maxPerImageVm, maxVmWaitSeconds,
                vps4ApiClient, adminClient, vps4ShopperId, threadPool);

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

    private static CommandLine parseCliArgs(String[] args) throws ParseException {

        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption( "a", "api-url", true, "vps4 api url that you plan to run against.  Example: http://127.0.0.1:8089/" );
        options.addOption( "s", "shopper", true, "vps4 shopperId" );
        options.addOption( "p", "password", true, "vps4 password" );
        options.addOption( "o", "sso-url", true, "sso url to get the admin token");
        options.addOption( "j", "admin", true, "admin (jomax) username" );
        options.addOption( "k", "admin-pass", true, "admin password");
        options.addOption( "m", "max-vms", true, "maximum number of vms to create");
        options.addOption( "p", "pool-size", true, "maximum number of vms per image type");
        options.addOption( "t", "vm-timeout", true, "maximum time in seconds a test will wait for a VM");

        CommandLine cmd = parser.parse(options, args);
        for (Option option : cmd.getOptions())
            logger.debug("Args: {} : {}", option.getLongOpt(), option.getValue());
        return cmd;
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
