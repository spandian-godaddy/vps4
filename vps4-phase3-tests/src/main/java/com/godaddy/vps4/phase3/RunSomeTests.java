package com.godaddy.vps4.phase3;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.api.SsoClient;
import com.godaddy.vps4.phase3.api.Vps4ApiClient;
import com.godaddy.vps4.phase3.tests.ActivationTest;
import com.godaddy.vps4.phase3.tests.AddMonitoringTest;
import com.godaddy.vps4.phase3.tests.AddSupportUserTest;
import com.godaddy.vps4.phase3.tests.ChangeHostnameTest;
import com.godaddy.vps4.phase3.tests.ConsoleUrlTest;
import com.godaddy.vps4.phase3.tests.EnableDisableAdminTest;
import com.godaddy.vps4.phase3.tests.GetServerActionsTest;
import com.godaddy.vps4.phase3.tests.NetworkConnectivityTest;
import com.godaddy.vps4.phase3.tests.SetPasswordTest;
import com.godaddy.vps4.phase3.tests.SnapshotTest;
import com.godaddy.vps4.phase3.tests.StopStartVmTest;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachinePool;

public class RunSomeTests {

    private static final Logger logger = LoggerFactory.getLogger(RunSomeTests.class);

    public static void main(String[] args) throws Exception{

        CommandLine cmd = parseCliArgs(args);
        String URL = cmd.getOptionValue("api-url");
        String ssoUrl = cmd.getOptionValue("sso-url");
        boolean keepVms = Boolean.parseBoolean(cmd.getOptionValue("keep-vms"));

        String vps4ShopperId = cmd.getOptionValue("shopper");
        String vps4Password = cmd.getOptionValue("password");

        int maxTotalVm = Integer.parseInt(cmd.getOptionValue("max-vms"));
        int maxPerImageVm = Integer.parseInt(cmd.getOptionValue("pool-size"));
        int maxVmWaitSeconds = Integer.parseInt(cmd.getOptionValue("vm-timeout"));
        int dcId = Integer.parseInt(cmd.getOptionValue("dc-id", "1"));

        String testsArg = cmd.getOptionValue("tests-to-run",
                "ChangeHostnameTest,StopStartVmTest,SnapshotTest,SetPasswordTest,AddSupportUserTest,EnableDisableAdminTest,ConsoleUrlTest,GetServerActionsTest,ActivationTest,AddMonitoringTest,NetworkConnectivityTest");
        String[] testsInput = testsArg.split(",");

        SsoClient ssoClient = new SsoClient(ssoUrl);

        Vps4ApiClient adminClient = null;
        if (cmd.hasOption("admin")) {
            String adminUser = cmd.getOptionValue("admin");
            String adminPassword = cmd.getOptionValue("admin-pass");
            String adminAuthHeader = ssoClient.getJomaxSsoToken(adminUser, adminPassword);
            adminClient = new Vps4ApiClient(URL, adminAuthHeader);
        }

        String vps4AuthHeader = ssoClient.getVps4SsoToken(vps4ShopperId, vps4Password);
        Vps4ApiClient vps4ApiClient = new Vps4ApiClient(URL, vps4AuthHeader);

        deleteAnyExistingVms(vps4ApiClient);

        ExecutorService threadPool = Executors.newCachedThreadPool();

        VirtualMachinePool vmPool = new VirtualMachinePool(maxTotalVm,
                                                           maxPerImageVm,
                                                           maxVmWaitSeconds,
                                                           vps4ApiClient,
                                                           adminClient,
                                                           vps4ShopperId,
                                                           threadPool,
                                                           dcId);

        List<VmTest> tests = MapInputToTests(testsInput);

        TestGroup vps4 = new TestGroup("VPS4 Phase3 Tests");

        Set<String> images = parseCliImages(cmd, vps4ApiClient);
        logger.info("Running tests with images: " + images);

        for (String image : images) {
            ImageTestGroup imageTestGroup = new ImageTestGroup(image);
            imageTestGroup.addTests(tests);
            vps4.add(imageTestGroup);
        }

        TestGroupExecution testGroupExecution = vps4.execute(threadPool, vmPool);

        try {
            testGroupExecution.await();
        } catch(Exception e) {
            testGroupExecution.status = TestStatus.FAIL;
        }

        threadPool.shutdown();

        try {
            threadPool.awaitTermination(1, TimeUnit.HOURS);
        } catch(InterruptedException e) {
            System.err.println("Interrupted waiting for test termination");
            vmPool.destroyAll();
            printResults(testGroupExecution);
            System.exit(1);
        }

        if(!keepVms) {
            vmPool.destroyAll();
        }
        printResults(testGroupExecution);
    }

    private static List<VmTest> MapInputToTests(String[] testsInput) {
        List<VmTest> tests = new ArrayList<>();

        for (String test : testsInput) {
            switch (test) {
                case "ChangeHostnameTest":
                    tests.add(new ChangeHostnameTest(randomHostname()));
                    break;
                case "StopStartVmTest":
                    tests.add(new StopStartVmTest());
                    break;
                case "SnapshotTest":
                    tests.add(new SnapshotTest());
                    break;
                case "SetPasswordTest":
                    tests.add(new SetPasswordTest(randomPassword(14)));
                    break;
                case "AddSupportUserTest":
                    tests.add(new AddSupportUserTest());
                    break;
                case "EnableDisableAdminTest":
                    tests.add(new EnableDisableAdminTest());
                    break;
                case "ConsoleUrlTest":
                    tests.add(new ConsoleUrlTest());
                    break;
                case "GetServerActionsTest":
                    tests.add(new GetServerActionsTest());
                    break;
                case "ActivationTest":
                    tests.add(new ActivationTest());
                    break;
                case "AddMonitoringTest":
                    tests.add(new AddMonitoringTest());
                    break;
                case "NetworkConnectivityTest":
                    tests.add(new NetworkConnectivityTest());
                    break;
            }
        }

        return tests;
    }

    private static void deleteAnyExistingVms(Vps4ApiClient vps4ApiClient) {
        List<UUID> vmsToDelete = vps4ApiClient.getListOfExistingVmIds();
        if (!vmsToDelete.isEmpty()) {
            System.out.println(String.format("Found %d existing VMs, deleting before running tests", vmsToDelete.size()));
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (UUID vmId : vmsToDelete) {
                futures.add(CompletableFuture.runAsync(() -> deleteVm(vps4ApiClient, vmId)));
            }
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
            }
            catch (Exception e) {
                e.printStackTrace();
                System.out
                        .println("Exception while deleting existing VMS, tests will continue to run but may fail due to lack of credits.");
            }
        }
    }

    private static void deleteVm(Vps4ApiClient vps4ApiClient, UUID vmId) {
        System.out.println("Deleting VM " + vmId);
        vps4ApiClient.deleteVm(vmId);
    }

    private static Set<String> parseCliImages(CommandLine cmd, Vps4ApiClient vps4ApiClient) {
        Set<String> images = new HashSet<>();
        String imagesParam = cmd.getOptionValue("images");
        if (imagesParam.length() > 0) {
            Collections.addAll(images, imagesParam.split(","));
        }
        if (cmd.hasOption("all-oh-images")) {
            JSONArray ohImages = vps4ApiClient.getImages("OPTIMIZED_HOSTING");
            for (Object ohImage : ohImages) {
                JSONObject image = (JSONObject) ohImage;
                images.add(image.get("hfsName").toString());
            }
        }
        return images;
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
        options.addOption( "d", "dc-id", true, "the id of the dc in which the phase3 tests are being run");
        options.addOption( null, "all-oh-images", false, "automatically include all active OH images");
        options.addOption( "i", "images", true, "additional hfs images to test");
        options.addOption( "r", "tests-to-run", true, "tests to run");
        options.addOption( "k", "keep-vms", true, "keep vms after the tests run");

        CommandLine cmd = parser.parse(options, args);
        for (Option option : cmd.getOptions())
            logger.debug("Args: {} : {}", option.getLongOpt(), option.getValue());
        return cmd;
    }

    private static void printResults(TestGroupExecution testGroupExecution){
        System.out.println("<<<<================================");
        testGroupExecution.printResults();
        System.out.println(">>>>================================");
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

     public static String randomPassword(int length) {
         List<CharacterRule> rules = new ArrayList<>();
         rules.add(new CharacterRule(EnglishCharacterData.UpperCase, 1));
         rules.add(new CharacterRule(EnglishCharacterData.LowerCase, 1));
         rules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
         CharacterData SPECIAL_CHAR_DATA = new CharacterData() {
             @Override
             public String getErrorCode() {
                 return "INVALID_SPECIAL_CHARS";
             }

             @Override
             public String getCharacters() { return "@!#%$"; }
         };
         rules.add(new CharacterRule(SPECIAL_CHAR_DATA, 1));

         PasswordGenerator pwGenerator = new PasswordGenerator();
         return pwGenerator.generatePassword(length, rules);
     }
}
