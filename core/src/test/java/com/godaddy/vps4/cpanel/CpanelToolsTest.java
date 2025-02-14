package com.godaddy.vps4.cpanel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;

public class CpanelToolsTest {

    Vps4CpanelService service;
    CpanelApiTokenService cpanelApiTokenService = mock(CpanelApiTokenService.class);
    NetworkService networkService = mock(NetworkService.class);
    Config config = mock(Config.class);
    CpanelClient cpClient = mock(CpanelClient.class);
    @Captor private ArgumentCaptor<String> domainArgumentCaptor;
    @Captor private ArgumentCaptor<String> usernameArgumentCaptor;
    @Captor private ArgumentCaptor<String> passwordArgumentCaptor;
    @Captor private ArgumentCaptor<String> planArgumentCaptor;
    @Captor private ArgumentCaptor<String> emailArgumentCaptor;
    @Captor private ArgumentCaptor<Boolean> enabledArgumentCaptor;
    @Captor private ArgumentCaptor<List<String>> usernamesArgumentCaptor;
    @Captor private ArgumentCaptor<String> keyArgumentCaptor;
    @Captor private ArgumentCaptor<String> valueArgumentCaptor;

    long hfsVmId = 1234;

    public class TestDefaultVps4CpanelService extends DefaultVps4CpanelService {
        public TestDefaultVps4CpanelService(CpanelApiTokenService apiTokenService,
                                            NetworkService networkService,
                                            int timeoutVal) {
            super(apiTokenService, networkService, timeoutVal);
        }

        @Override
        protected CpanelClient getCpanelClient(String publicIp, String apiToken){
            return cpClient;
        }
    }

    @Before
    public void setupTest(){
        when(config.get("vps4.callable.timeout", "10000")).thenReturn("10");

        UUID vmId = UUID.randomUUID();
        IpAddress ip = new IpAddress(1, 1, vmId, "123.0.0.1", IpAddressType.PRIMARY, null, null, 4);
        when(networkService.getVmPrimaryAddress(hfsVmId)).thenReturn(ip);
        when(cpanelApiTokenService.getApiToken(eq(hfsVmId), any())).thenReturn("randomapitoken");

        service = new TestDefaultVps4CpanelService(cpanelApiTokenService, networkService, 10);
        MockitoAnnotations.initMocks(this);
    }

    @Test()
    public void testCallsGetApiTokenService() throws Exception{
        when(cpClient.createSession( "root", CpanelClient.CpanelServiceType.whostmgrd)).thenReturn(new CPanelSession());
        service.createSession(hfsVmId, "root", CpanelClient.CpanelServiceType.whostmgrd);
        verify(cpanelApiTokenService, times(1)).getApiToken(eq(hfsVmId), any());
    }

    @Test()
    public void testListAddonDomainsNullResult() throws Exception{
        String cpanelReturnVal = "{\"cpanelresult\":null}";
        when(cpClient.listAddOnDomains("fakeUsername")).thenReturn(cpanelReturnVal);
        List<String> returnValue = new ArrayList<String>();
        assertEquals(returnValue, service.listAddOnDomains(hfsVmId, "fakeUsername"));
    }

    @Test(expected=CpanelInvalidUserException.class)
    public void testListAddonDomainsInvalidUsername() throws Exception{
        String cpanelReturnVal = "{\"cpanelresult\":{\"data\":{"
                + "\"reason\":\"User parameter is invalid or was not supplied\","
                + "\"result\":\"0\"},"
                + "\"type\":\"text\","
                + "\"error\":\"User parameter is invalid or was not supplied\"}}";
        when(cpClient.listAddOnDomains("fakeUsername")).thenReturn(cpanelReturnVal);
        service.listAddOnDomains(hfsVmId, "fakeUsername");
    }

    @Test(expected=CpanelTimeoutException.class)
    public void testListAddonDomainsOtherError() throws Exception{
        String cpanelReturnVal = "{\"cpanelresult\":{\"data\":{"
                + "\"reason\":\"Another of the many reasons cpanel might fail\","
                + "\"result\":\"0\"},"
                + "\"type\":\"text\","
                + "\"error\":\"Another of the many reasons cpanel might fail\"}}";
        when(cpClient.listAddOnDomains("fakeUsername")).thenReturn(cpanelReturnVal);
        service.listAddOnDomains(hfsVmId, "fakeUsername");
    }

    @Test()
    public void testListAddonDomainsSuccess() throws Exception{
        String cpanelReturnVal = "{\"cpanelresult\":{\"func\":\"listaddondomains\","
                + "\"event\":{\"result\":1},"
                + "\"apiversion\":2,"
                + "\"data\":[{\"rootdomain\":\"new.account.acc\","
                + "\"domainkey\":\"newfake_new.account.acc\","
                + "\"basedir\":\"public_html/newfake.domain\","
                + "\"subdomain\":\"newfake\","
                + "\"domain\":\"newfake.domain\","
                + "\"fullsubdomain\":\"newfake.new.account.acc\","
                + "\"web_subdomain_aliases\":[\"www\",\"mail\"],"
                + "\"status\":\"not redirected\","
                + "\"dir\":\"/home/newaccount/public_html/newfake.domain\","
                + "\"reldir\":\"home:public_html/newfake.domain\"},"
                + "{\"web_subdomain_aliases\":[\"www\",\"mail\"],"
                + "\"subdomain\":\"second\",\"domain\":\"second.domain\",\"fullsubdomain\":\"second.new.account.acc\","
                + "\"status\":\"not redirected\",\"reldir\":\"home:public_html/second.domain\","
                + "\"dir\":\"/home/newaccount/public_html/second.domain\",\"rootdomain\":\"new.account.acc\","
                + "\"domainkey\":\"second_new.account.acc\",\"basedir\":\"public_html/second.domain\"}],"
                + "\"module\":\"AddonDomain\"}}";
        when(cpClient.listAddOnDomains("fakeUsername")).thenReturn(cpanelReturnVal);
        service.listAddOnDomains(hfsVmId, "fakeUsername");
        List<String> returnValue = new ArrayList<String>();
        returnValue.add("newfake.domain");
        returnValue.add("second.domain");
        assertEquals(returnValue, service.listAddOnDomains(hfsVmId, "fakeUsername"));
    }

    @Test()
    public void testListAddonDomainsSuccessAlphabetical() throws Exception {
        String cpanelReturnVal = "{\"cpanelresult\":{\"data\":[{\"domain\":\"third.domain\"},{\"domain\":\"first" +
                ".domain\"},{\"domain\":\"second.domain\"}]}}";
        when(cpClient.listAddOnDomains("fakeUsername")).thenReturn(cpanelReturnVal);
        service.listAddOnDomains(hfsVmId, "fakeUsername");
        List<String> returnValue = new ArrayList<>();
        returnValue.add("first.domain");
        returnValue.add("second.domain");
        returnValue.add("third.domain");
        assertEquals(returnValue, service.listAddOnDomains(hfsVmId, "fakeUsername"));
    }

    @Test()
    public void testListAddonDomainsSuccessNoAddons() throws Exception{
        String cpanelReturnVal = "{\"cpanelresult\":{\"func\":\"listaddondomains\","
                + "\"event\":{\"result\":1},"
                + "\"apiversion\":2,"
                + "\"data\":[],"
                + "\"module\":\"AddonDomain\"}}";
        when(cpClient.listAddOnDomains("fakeUsername")).thenReturn(cpanelReturnVal);
        service.listAddOnDomains(hfsVmId, "fakeUsername");
        List<String> returnValue = new ArrayList<String>();
        assertEquals(returnValue, service.listAddOnDomains(hfsVmId, "fakeUsername"));
    }

    @Test()
    public void testListDomainsSuccessZeroDomains() throws Exception {
        String returnVal = "{\"metadata\":{\"result\":1,\"reason\":\"OK\"," +
                "\"version\":1,\"command\":\"get_domain_info\"},\"data\":" +
                "{\"domains\":[]}}";
        when(cpClient.listDomains(CPanelDomainType.ADDON)).thenReturn(returnVal);
        List<CPanelDomain> returnValue = new ArrayList<>();
        assertEquals(returnValue, service.listDomains(hfsVmId, CPanelDomainType.ADDON));
    }

    @Test()
    public void testListDomainsSuccess() throws Exception {
        String returnVal = "{\"metadata\":{\"result\":1,\"reason\":\"OK\"," +
                "\"version\":1,\"command\":\"get_domain_info\"},\"data\":" +
                "{\"domains\":[{\"domain_type\":\"addon\",\"domain\":\"hello.fake\",\"user\":\"testuser\"}," +
                "{\"domain_type\":\"addon\",\"domain\":\"hello2.fake\",\"user\":\"testuser2\"}]}}";
        when(cpClient.listDomains(CPanelDomainType.ADDON)).thenReturn(returnVal);
        List<CPanelDomain> domains = service.listDomains(hfsVmId, CPanelDomainType.ADDON);

        assertEquals(2, domains.size());
        assertEquals("hello.fake", domains.get(0).domainName);
        assertEquals("testuser", domains.get(0).username);
        assertEquals("addon", domains.get(0).domainType);
        assertEquals("hello2.fake", domains.get(1).domainName);
        assertEquals("testuser2", domains.get(1).username);
        assertEquals("addon", domains.get(1).domainType);
    }

    @Test
    public void testListDomainsError() throws Exception{
        String reason = "no-workie";
        String returnVal = "{\"metadata\":{\"version\":1,\"reason\":\"" + reason + "\", \"result\":0}}";
        when(cpClient.listDomains(CPanelDomainType.ALL)).thenReturn(returnVal);

        try {
            service.listDomains(hfsVmId, CPanelDomainType.ALL);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM list domains by type failed due to reason: " + reason, e.getMessage());
        }
    }


    @Test
    public void calculatePasswordStrengthUsesCpanelClient() throws Exception{
        String password = "password123";
        String returnVal = "{\"data\":{\"strength\":31},\"metadata\":{\"version\":1,"
            + "\"command\":\"get_password_strength\",\"reason\":\"OK\",\"result\":1}}";
        when(cpClient.calculatePasswordStrength(password)).thenReturn(returnVal);
        service.calculatePasswordStrength(hfsVmId, password);
        verify(cpClient, times(1)).calculatePasswordStrength(passwordArgumentCaptor.capture());
        Assert.assertEquals(password, passwordArgumentCaptor.getValue());
    }

    @Test
    public void returnsPasswordStrengthGotFromCpanelClient() throws Exception{
        Long passwordStrength = 31L;
        String password = "password123";
        String returnVal = "{\"data\":{\"strength\":" + passwordStrength + "},\"metadata\":{\"version\":1,"
                + "\"command\":\"get_password_strength\",\"reason\":\"OK\",\"result\":1}}";
        when(cpClient.calculatePasswordStrength(password)).thenReturn(returnVal);
        Long returnedStrength = service.calculatePasswordStrength(hfsVmId, password);
        Assert.assertEquals(passwordStrength, returnedStrength);
    }

    @Test(expected=CpanelTimeoutException.class)
    public void calculatePasswordStrengthParseException() throws Exception {
        String password = "password123";
        String returnVal = "{'not-valid': 'json'}";
        when(cpClient.calculatePasswordStrength(password)).thenReturn(returnVal);
        service.calculatePasswordStrength(hfsVmId, password);
    }

    @Test
    public void calculatePasswordStrengthNoMetadata() throws Exception {
        String password = "password123";
        String returnVal = "{\"data\":{\"strength\":31}, \"metadata\": null}";
        when(cpClient.calculatePasswordStrength(password)).thenReturn(returnVal);

        try {
            service.calculatePasswordStrength(hfsVmId, password);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("Password strength calculation failed due to reason: No reason provided", e.getMessage());
        }
    }

    @Test
    public void calculatePasswordStrengthResultNotOk() throws Exception {
        String password = "password123";
        String reason = "no-workie";
        String returnVal = "{\"metadata\":{\"version\":1,\"reason\":\"" + reason + "\", \"result\":0}}";
        when(cpClient.calculatePasswordStrength(password)).thenReturn(returnVal);

        try {
            service.calculatePasswordStrength(hfsVmId, password);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("Password strength calculation failed due to reason: " + reason, e.getMessage());
        }
    }

    @Test
    public void calculatePasswordStrengthNullData() throws Exception {
        String password = "password123";
        String returnVal = "{\"data\": null, \"metadata\":{\"version\":1,\"reason\":\"OK\", \"result\":1}}";
        when(cpClient.calculatePasswordStrength(password)).thenReturn(returnVal);

        try {
            service.calculatePasswordStrength(hfsVmId, password);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("Error while handling response for call calculatePasswordStrength", e.getMessage());
        }
    }

    @Test
    public void createAccountUsesCpanelClient() throws Exception{
        String domainName = "domain";
        String username = "username";
        String password = "password123";
        String plan = "plan";
        String email = "email@email.com";
        String returnVal = "{\"data\":{},\"metadata\":{\"version\":1,"
                + "\"command\":\"get_password_strength\",\"reason\":\"OK\",\"result\":1}}";
        when(cpClient.createAccount(domainName, username, password, plan, email)).thenReturn(returnVal);
        service.createAccount(hfsVmId, domainName, username, password, plan, email);
        verify(cpClient, times(1))
            .createAccount(domainArgumentCaptor.capture(), usernameArgumentCaptor.capture(),
                passwordArgumentCaptor.capture(), planArgumentCaptor.capture(), emailArgumentCaptor.capture());
        Assert.assertEquals(domainName, domainArgumentCaptor.getValue());
        Assert.assertEquals(username, usernameArgumentCaptor.getValue());
        Assert.assertEquals(password, passwordArgumentCaptor.getValue());
        Assert.assertEquals(email, emailArgumentCaptor.getValue());
        Assert.assertEquals(plan, planArgumentCaptor.getValue());
    }

    @Test
    public void createAccountNoMetadata() throws Exception {
        String domainName = "domain";
        String username = "username";
        String password = "password123";
        String plan = "plan";
        String email = "email@email.com";
        String returnVal = "{\"data\":{}, \"metadata\": null}";
        when(cpClient.createAccount(domainName, username, password, plan, email)).thenReturn(returnVal);

        try {
            service.createAccount(hfsVmId, domainName, username, password, plan, email);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM account creation failed due to reason: No reason provided", e.getMessage());
        }
    }

    @Test
    public void createAccountResultNotOk() throws Exception {
        String domainName = "domain";
        String username = "username";
        String password = "password123";
        String plan = "plan";
        String reason = "no-workie";
        String email = "email@email.com";
        String returnVal = "{\"metadata\":{\"version\":1,\"reason\":\"" + reason + "\", \"result\":0}}";
        when(cpClient.createAccount(domainName, username, password, plan, email)).thenReturn(returnVal);

        try {
            service.createAccount(hfsVmId, domainName, username, password, plan, email);
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM account creation failed due to reason: " + reason, e.getMessage());
        }
    }

    @Test
    public void addAddOnDomainUsesCpanelClient() throws Exception {
        String username = "vpsdev";
        String newDomain = "test.com";
        String returnVal = "{\"cpanelresult\":{\"preevent\":{\"result\":1},\"data\":[{\"result\":1,\"reason\":\"The " +
                "system successfully parked (aliased) the domain “test.com” on top of the domain " +
                "“test.com.vpsdev.net”.\"}],\"postevent\":{\"result\":1},\"module\":\"AddonDomain\"," +
                "\"func\":\"addaddondomain\",\"apiversion\":2,\"event\":{\"result\":1}}}";
        when(cpClient.addAddOnDomain(username, newDomain)).thenReturn(returnVal);

        service.addAddOnDomain(hfsVmId, username, newDomain);

        verify(cpClient, times(1))
                .addAddOnDomain(usernameArgumentCaptor.capture(), domainArgumentCaptor.capture());
        Assert.assertEquals(username, usernameArgumentCaptor.getValue());
        Assert.assertEquals(newDomain, domainArgumentCaptor.getValue());
    }

    @Test
    public void addAddOnDomainResultNotOk() throws Exception {
        String username = "vpsdev";
        String newDomain = "test.com";
        String reason = "(XID xbn4z2) The domain test.com already exists in the userdata.";
        String returnVal = "{\"cpanelresult\":{\"event\":{\"result\":1},\"apiversion\":2,\"func\":\"addaddondomain\"," +
                "\"module\":\"AddonDomain\",\"postevent\":{\"result\":1},\"error\":\"" + reason + "\"," +
                "\"data\":[{\"result\":0,\"reason\":\"" + reason + "\"}],\"preevent\":{\"result\":1}}}";
        when(cpClient.addAddOnDomain(username, newDomain)).thenReturn(returnVal);

        String response = service.addAddOnDomain(hfsVmId, username, newDomain);

        Assert.assertEquals(reason, response);
    }

    @Test
    public void installRpmPackagesUsesCpanelClient() throws Exception{
        String packageName = "testPackage";
        String returnVal = "{\"metadata\":{\"reason\":\"OK\",\"version\":1,\"result\":1,\"command\":\"listpkgs\"},"
            + "\"data\":{\"build\":12345}}";
        when(cpClient.installRpmPackage(packageName)).thenReturn(returnVal);
        service.installRpmPackage(hfsVmId, packageName);
        verify(cpClient, times(1)).installRpmPackage(packageName);
    }

    @Test
    public void installRpmPackagesReturnsCPanelBuildObject() throws Exception{
        String packageName = "testPackage";
        String returnVal = "{\"metadata\":{\"reason\":\"OK\",\"version\":1,\"result\":1,\"command\":\"listpkgs\"},"
                + "\"data\":{\"build\":12345}}";
        when(cpClient.installRpmPackage(packageName)).thenReturn(returnVal);
        CpanelBuild build = service.installRpmPackage(hfsVmId, packageName);
        Assert.assertEquals(12345, build.buildNumber);
        Assert.assertEquals(packageName, build.packageName);
    }

    @Test
    public void installRpmPackagesNoMetadata() throws Exception {
        String packageName = "testPackage";
        String returnVal = "{\"data\":{\"blah\": \"foo\"}, \"metadata\": null}";
        when(cpClient.installRpmPackage(packageName)).thenReturn(returnVal);

        try {
            service.installRpmPackage(hfsVmId, packageName);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM install rpm package failed due to reason: No reason provided", e.getMessage());
        }
    }

    @Test
    public void installRpmPackagesResultNotOk() throws Exception {
        String packageName = "testPackage";
        String reason = "no-workie";
        String returnVal = "{\"metadata\":{\"version\":1,\"reason\":\"" + reason + "\", \"result\":0}}";
        when(cpClient.installRpmPackage(packageName)).thenReturn(returnVal);

        try {
            service.installRpmPackage(hfsVmId, packageName);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM install rpm package failed due to reason: " + reason, e.getMessage());
        }
    }

    @Test
    public void installRpmPackagesNullData() throws Exception {
        String packageName = "testPackage";
        String returnVal = "{\"data\": null, \"metadata\":{\"version\":1,\"reason\":\"OK\", \"result\":1}}";
        when(cpClient.installRpmPackage(packageName)).thenReturn(returnVal);

        try {
            service.installRpmPackage(hfsVmId, packageName);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("Error while handling response for call installRpmPackage", e.getMessage());
        }
    }

    @Test
    public void installRpmPackagesNullBuild() throws Exception {
        String packageName = "testPackage";
        String returnVal = "{\"data\": {\"build\": null}, \"metadata\":{\"version\":1,\"reason\":\"OK\", \"result\":1}}";
        when(cpClient.installRpmPackage(packageName)).thenReturn(returnVal);

        try {
            service.installRpmPackage(hfsVmId, packageName);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM install rpm package failed: build returned null", e.getMessage());
        }
    }

    @Test
    public void listRpmPackagesUsesCpanelClient() throws Exception{
        String returnVal = "{\"metadata\":{\"reason\":\"OK\",\"command\":\"package_manager_list_packages\",\"version\":1," +
                "\"result\":1},\"data\":{\"packages\":[{\"package\":\"testPackage1\"},{\"package\":\"testPackage2\"}," +
                "{\"package\":\"testPackage3\"},{\"package\":\"testPackage4\"}]}}";
        when(cpClient.listInstalledRpmPackages()).thenReturn(returnVal);
        service.listInstalledRpmPackages(hfsVmId);
        verify(cpClient, times(1)).listInstalledRpmPackages();
    }

    @Test
    public void listRpmPackagesReturnsCPanelBuildObject() throws Exception{
        String returnVal = "{\"metadata\":{\"reason\":\"OK\",\"command\":\"package_manager_list_packages\",\"version\":1," +
                "\"result\":1},\"data\":{\"packages\":[{\"package\":\"testPackage1\"},{\"package\":\"testPackage2\"}," +
                "{\"package\":\"testPackage3\"},{\"package\":\"testPackage4\"}]}}";
        when(cpClient.listInstalledRpmPackages()).thenReturn(returnVal);
        List<String> packages = service.listInstalledRpmPackages(hfsVmId);
        List<String> expectedPackages = Arrays.asList("testPackage1","testPackage2", "testPackage3", "testPackage4");
        Assert.assertEquals(expectedPackages, packages);
    }

    @Test
    public void listRpmPackagesNoMetadata() throws Exception {
        String returnVal = "{\"data\":{\"blah\": \"foo\"}, \"metadata\": null}";
        when(cpClient.listInstalledRpmPackages()).thenReturn(returnVal);

        try {
            service.listInstalledRpmPackages(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM list installed rpm packages failed due to reason: No reason provided", e.getMessage());
        }
    }

    @Test
    public void listRpmPackagesResultNotOk() throws Exception {
        String reason = "no-workie";
        String returnVal = "{\"metadata\":{\"version\":1,\"reason\":\"" + reason + "\", \"result\":0}}";
        when(cpClient.listInstalledRpmPackages()).thenReturn(returnVal);

        try {
            service.listInstalledRpmPackages(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM list installed rpm packages failed due to reason: " + reason, e.getMessage());
        }
    }

    @Test
    public void listRpmPackagesNullData() throws Exception {
        String returnVal = "{\"data\": null, \"metadata\":{\"version\":1,\"reason\":\"OK\", \"result\":1}}";
        when(cpClient.listInstalledRpmPackages()).thenReturn(returnVal);

        try {
            service.listInstalledRpmPackages(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("Error while handling response for call listInstalledRpms", e.getMessage());
        }
    }

    @Test
    public void listRpmPackagesNullBuild() throws Exception {
        String returnVal = "{\"metadata\":{\"reason\":\"OK\",\"command\":\"package_manager_list_packages\",\"version\":1," +
                "\"result\":1},\"data\":{\"packages\":null}}";
        when(cpClient.listInstalledRpmPackages()).thenReturn(returnVal);

        try {
            service.listInstalledRpmPackages(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("No installed cpanel rpm packages present", e.getMessage());
        }
    }

    @Test
    public void listInstallatronAppsUsesCpanelClient() throws Exception{
        String returnVal = "{\"result\":true,\"message\":\"The task is complete.\\n\",\"errcode\":null," +
                "\"errfield\":null,\"data\":[{\"id\":\"testId\"," +
                "\"file\":\"\\/home\\/dtstus\\/.appdata\\/current\\/testId\",\"installer\":\"sitebar\"," +
                "\"version\":\"3.6.2\",\"owner\":\"dtstus\",\"path\":\"\\/home\\/dtstus\\/public_html\\/sitebar\"," +
                "\"url\":\"http:\\/\\/www.tester.fake\\/sitebar\",\"url-domain\":\"tester.fake\",\"title\":\"My bookmarks\"}]}";
        when(cpClient.listInstalledInstallatronApplications("fakeUsername")).thenReturn(returnVal);
        service.listInstalledInstallatronApplications(hfsVmId, "fakeUsername");
        verify(cpClient, times(1)).listInstalledInstallatronApplications("fakeUsername");
    }

    @Test
    public void listInstallatronAppsReturnsInstallatronList() throws Exception{
        String returnVal = "{\"result\":true,\"message\":\"The task is complete.\\n\",\"errcode\":null," +
                "\"errfield\":null,\"data\":[{\"id\":\"testId\"," +
                "\"file\":\"\\/home\\/dtstus\\/.appdata\\/current\\/testId\",\"installer\":\"testApp\"," +
                "\"version\":\"3.6.2\",\"owner\":\"dtstus\",\"path\":\"\\/home\\/dtstus\\/public_html\\/sitebar\"," +
                "\"url\":\"http:\\/\\/www.tester.fake\\/sitebar\",\"url-domain\":\"tester.fake\",\"title\":\"My bookmarks\"}]}";
        when(cpClient.listInstalledInstallatronApplications("fakeUsername")).thenReturn(returnVal);
        List<InstallatronApplication> apps = service.listInstalledInstallatronApplications(hfsVmId,"fakeUsername");
        Assert.assertEquals(1, apps.size());
        Assert.assertEquals("testApp", apps.get(0).name);
        Assert.assertEquals("testId", apps.get(0).id);
        Assert.assertEquals("http://www.tester.fake/sitebar", apps.get(0).domain);
        Assert.assertEquals("tester.fake", apps.get(0).urlDomain);
        Assert.assertEquals("3.6.2", apps.get(0).version);
    }

    @Test
    public void listInstallatronAppsResultFalse() throws Exception {
        String returnVal =  "{\"result\":false,\"message\":\"Error: Missing argument\\n\"," +
                "\"errcode\":\"empty_argument_application\",\"errfield\":null,\"data\":null}";
        when(cpClient.listInstalledInstallatronApplications("fakeUsername")).thenReturn(returnVal);

        try {
            service.listInstalledInstallatronApplications(hfsVmId,"fakeUsername");
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("Error querying Installatron for list of installed applications" +
                    " due to reason: Error: Missing argument\n", e.getMessage());
        }
    }

    @Test(expected = RuntimeException.class)
    public void listInstallatronAppsNullData() throws Exception {
        String returnVal =  "{\"result\":true,\"message\":\"The task is complete.\\n\",\"errcode\":null,\"errfield\":null,\"data\":null}";
        when(cpClient.listInstalledInstallatronApplications("fakeUsername")).thenReturn(returnVal);
        service.listInstalledInstallatronApplications(hfsVmId,"fakeUsername");
    }

    @Test
    public void listInstallatronAppsEmptyData() throws Exception {
        String returnVal = "{\"result\":true,\"message\":\"The task is complete.\\n\",\"errcode\":null,\"errfield\":null,\"data\":[]}";
        when(cpClient.listInstalledInstallatronApplications("fakeUsername")).thenReturn(returnVal);
        List<InstallatronApplication> apps = service.listInstalledInstallatronApplications(hfsVmId,"fakeUsername");
        Assert.assertEquals(0, apps.size());
    }

    @Test
    public void getActiveBuildsUsesCpanelClient() throws Exception{
        String returnVal = "{\"data\":{\"active\":0},\"metadata\":{\"version\":1," +
                "\"command\":\"package_manager_is_performing_actions\",\"reason\":\"OK\",\"result\":1}}";
        when(cpClient.getRpmPackageUpdateStatus("12345")).thenReturn(returnVal);
        service.getActiveBuilds(hfsVmId, 12345);
        verify(cpClient, times(1)).getRpmPackageUpdateStatus("12345");
    }

    @Test
    public void getActiveBuildsReturnsNumberOfActiveBuilds() throws Exception{
        String returnVal = "{\"data\":{\"active\":1},\"metadata\":{\"version\":1," +
                "\"command\":\"package_manager_is_performing_actions\",\"reason\":\"OK\",\"result\":1}}";
        when(cpClient.getRpmPackageUpdateStatus("12345")).thenReturn(returnVal);
        long activeBuilds = service.getActiveBuilds(hfsVmId, 12345);
        Assert.assertEquals(1L, activeBuilds);
    }

    @Test
    public void getActiveBuildsNoMetadata() throws Exception {
        String returnVal = "{\"data\":{\"blah\": \"foo\"}, \"metadata\": null}";
        when(cpClient.getRpmPackageUpdateStatus("12345")).thenReturn(returnVal);

        try {
            service.getActiveBuilds(hfsVmId, 12345);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM get Rpm Package Update Status failed due to reason: No reason provided", e.getMessage());
        }
    }

    @Test
    public void getActiveBuildsResultNotOk() throws Exception {
        String reason = "no-workie";
        String returnVal = "{\"metadata\":{\"version\":1,\"reason\":\"" + reason + "\", \"result\":0}}";
        when(cpClient.getRpmPackageUpdateStatus("12345")).thenReturn(returnVal);

        try {
            service.getActiveBuilds(hfsVmId, 12345);
            Assert.fail("This test shouldn't get here");
        } catch (RuntimeException e) {
            Assert.assertEquals("WHM get Rpm Package Update Status failed due to reason: " + reason, e.getMessage());
        }
    }

    @Test
    public void getActiveBuildsNullData() throws Exception {
        String returnVal = "{\"data\": null, \"metadata\":{\"version\":1,\"reason\":\"OK\", \"result\":1}}";
        when(cpClient.getRpmPackageUpdateStatus("12345")).thenReturn(returnVal);

        try {
            service.getActiveBuilds(hfsVmId, 12345);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("Error while handling response for call getRpmPackageUpdateStatus", e.getMessage());
        }
    }

    @Test
    public void getActiveBuildsNullBuild() throws Exception {
        String returnVal = "{\"data\":{\"active\":null},\"metadata\":{\"version\":1," +
                "\"command\":\"package_manager_is_performing_actions\",\"reason\":\"OK\",\"result\":1}}";
        when(cpClient.getRpmPackageUpdateStatus("12345")).thenReturn(returnVal);

        try {
            service.getActiveBuilds(hfsVmId, 12345);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("No active builds found - number of builds returned null", e.getMessage());
        }
    }

    @Test
    public void getVersionUsesCpanelClient() throws Exception{
        String returnVal = "{\"metadata\":{\"result\":1,\"reason\":\"OK\",\"version\":1,\"command\":\"version\"}," +
                "\"data\":{\"version\":\"11.106.0.8\"}}";
        when(cpClient.getVersion()).thenReturn(returnVal);
        service.getVersion(hfsVmId);
        verify(cpClient, times(1)).getVersion();
    }

    @Test
    public void getVersionReturnsVersion() throws Exception{
        String returnVal = "{\"metadata\":{\"result\":1,\"reason\":\"OK\",\"version\":1,\"command\":\"version\"}," +
                "\"data\":{\"version\":\"11.106.0.8\"}}";
        when(cpClient.getVersion()).thenReturn(returnVal);
        String version = service.getVersion(hfsVmId);
        Assert.assertEquals("11.106.0.8", version);
    }

    @Test
    public void getVersionNoMetadata() throws Exception {
        String returnVal = "{\"metadata\":null,\"data\":{\"version\":\"11.106.0.8\"}}";
        when(cpClient.getVersion()).thenReturn(returnVal);

        try {
            service.getVersion(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM get Rpm version failed due to reason: No reason provided", e.getMessage());
        }
    }

    @Test
    public void getVersionResultNotOk() throws Exception {
        String reason = "no-workie";
        String returnVal = "{\"metadata\":{\"version\":1,\"reason\":\"" + reason + "\", \"result\":0}}";
        when(cpClient.getVersion()).thenReturn(returnVal);

        try {
            service.getVersion(hfsVmId);
            Assert.fail("This test shouldn't get here");
        } catch (RuntimeException e) {
            Assert.assertEquals("WHM get Rpm version failed due to reason: " + reason, e.getMessage());
        }
    }

    @Test
    public void getVersionNullData() throws Exception {
        String returnVal = "{\"data\": null, \"metadata\":{\"version\":1,\"reason\":\"OK\", \"result\":1}}";
        when(cpClient.getVersion()).thenReturn(returnVal);

        try {
            service.getVersion(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("Error while handling response for call getVersion", e.getMessage());
        }
    }

    @Test
    public void getVersionNullVersion() throws Exception {
        String returnVal = "{\"metadata\":{\"result\":1,\"reason\":\"OK\",\"version\":1,\"command\":\"version\"}," +
                "\"data\":{\"version\":null}}";
        when(cpClient.getVersion()).thenReturn(returnVal);

        try {
            service.getVersion(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("No version found - version data returned null", e.getMessage());
        }
    }

    @Test
    public void getNginxCacheConfigUsesCpanelClient() throws Exception {
        String returnVal = "{\"data\":{\"users\":[{\"user\":\"dieptran\",\"config\":{\"x_cache_header\":false," +
                "\"enabled\":true,\"proxy_cache_use_stale\":\"error timeout http_429 http_500 http_502 http_503 http_504\"," +
                "\"logging\":false,\"proxy_cache_min_uses\":1},\"owner\":\"root\",\"merged\":\"1\"}]}," +
                "\"metadata\":{\"reason\":\"OK\",\"version\":1,\"command\":\"nginxmanager_get_cache_config_users\",\"result\":1}}";
        when(cpClient.getNginxCacheConfig()).thenReturn(returnVal);
        service.getNginxCacheConfig(hfsVmId);
        verify(cpClient, times(1)).getNginxCacheConfig();
    }

    @Test
    public void getNginxCacheConfigReturnsEnabledConfig() throws Exception {
        String returnVal = "{\"data\":{\"users\":[{\"user\":\"dieptran\",\"config\":{\"x_cache_header\":false," +
                "\"enabled\":true,\"proxy_cache_use_stale\":\"error timeout http_429 http_500 http_502 http_503 http_504\"," +
                "\"logging\":false,\"proxy_cache_min_uses\":1},\"owner\":\"root\",\"merged\":\"1\"}]}," +
                "\"metadata\":{\"reason\":\"OK\",\"version\":1,\"command\":\"nginxmanager_get_cache_config_users\",\"result\":1}}";
        when(cpClient.getNginxCacheConfig()).thenReturn(returnVal);
        List<CPanelAccountCacheStatus> accountCacheStatusList = service.getNginxCacheConfig(hfsVmId);
        Assert.assertEquals(1, accountCacheStatusList.size());
        Assert.assertEquals(true, accountCacheStatusList.get(0).isEnabled);
        Assert.assertEquals("dieptran", accountCacheStatusList.get(0).username);
    }

    @Test
    public void getNginxCacheConfigNoMetadata() throws Exception {
        String returnVal = "{\"data\":{\"blah\": \"foo\"}, \"metadata\": null}";
        when(cpClient.getNginxCacheConfig()).thenReturn(returnVal);

        try {
            service.getNginxCacheConfig(hfsVmId);
            Assert.fail("This test shouldn't get here");
        } catch (RuntimeException e) {
            Assert.assertEquals("WHM get nginx cache config failed due to reason: No reason provided", e.getMessage());
        }
    }

    @Test
    public void getNginxCacheConfigResultNotOk() throws Exception {
        String reason = "no-workie";
        String returnVal = "{\"metadata\":{\"version\":1,\"reason\":\"" + reason + "\", \"result\":0}}";
        when(cpClient.getNginxCacheConfig()).thenReturn(returnVal);

        try {
            service.getNginxCacheConfig(hfsVmId);
            Assert.fail("This test shouldn't get here");
        } catch (RuntimeException e) {
            Assert.assertEquals("WHM get nginx cache config failed due to reason: " + reason, e.getMessage());
        }
    }


    @Test
    public void getNginxCacheConfigNullData() throws Exception {
        String returnVal = "{\"data\": null, \"metadata\":{\"version\":1,\"reason\":\"OK\", \"result\":1}}";
        when(cpClient.getNginxCacheConfig()).thenReturn(returnVal);

        try {
            service.getNginxCacheConfig(hfsVmId);
            Assert.fail("This test shouldn't get here");
        } catch (RuntimeException e) {
            Assert.assertEquals("Error while handling response for call getCacheConfig", e.getMessage());
        }
    }

    @Test
    public void getNginxCacheConfigNullUsersList() throws Exception {
        String returnVal = "{\"metadata\":{\"result\":1,\"reason\":\"OK\",\"version\":1,\"command\":\"version\"}," +
                "\"data\":{\"users\":null}}";
        when(cpClient.getNginxCacheConfig()).thenReturn(returnVal);

        try {
            service.getNginxCacheConfig(hfsVmId);
            Assert.fail("This test shouldn't get here");
        } catch (RuntimeException e) {
            Assert.assertEquals("No nginx cache config found - users list returned null", e.getMessage());
        }
    }

    @Test
    public void listPackagesUsesCpanelClient() throws Exception {
        String returnVal = "{\"metadata\":{\"reason\":\"OK\",\"version\":1,\"result\":1,\"command\":\"listpkgs\"},"
                + "\"data\":{\"pkg\":[{\"CPMOD\":\"paper_lantern\",\"IP\":\"n\",\"LANG\":\"en\",\"DIGESTAUTH\":\"n\","
                + "\"FEATURELIST\":\"default\",\"name\":\"default\"},{\"QUOTA\":\"unlimited\","
                + "\"MAX_EMAIL_PER_HOUR\":\"unlimited\",\"name\":\"test-package-1\",\"FEATURELIST\":\"default\","
                + "\"CGI\":\"y\",\"MAX_DEFER_FAIL_PERCENTAGE\":\"unlimited\",\"MAXSQL\":\"unlimited\",\"MAXPARK\":\"0\""
                + ",\"MAXADDON\":\"0\",\"HASSHELL\":\"n\",\"MAXLST\":\"unlimited\",\"_PACKAGE_EXTENSIONS\":\"\","
                + "\"LANG\":\"en\",\"MAXFTP\":\"unlimited\",\"MAXPOP\":\"unlimited\",\"BWLIMIT\":\"unlimited\","
                + "\"DIGESTAUTH\":\"n\",\"IP\":\"n\",\"CPMOD\":\"paper_lantern\",\"MAXSUB\":\"unlimited\"}]}}";
        when(cpClient.listPackages()).thenReturn(returnVal);
        service.listPackages(hfsVmId);
        verify(cpClient, times(1)).listPackages();
    }

    @Test
    public void returnsOnlyNameOfPackagesGotFromCpanelClient() throws Exception{
        String returnVal = "{\"metadata\":{\"reason\":\"OK\",\"version\":1,\"result\":1,\"command\":\"listpkgs\"},"
                + "\"data\":{\"pkg\":[{\"CPMOD\":\"paper_lantern\",\"IP\":\"n\",\"LANG\":\"en\",\"DIGESTAUTH\":\"n\","
                + "\"FEATURELIST\":\"default\",\"name\":\"default\"},{\"QUOTA\":\"unlimited\","
                + "\"MAX_EMAIL_PER_HOUR\":\"unlimited\",\"name\":\"test-package-1\",\"FEATURELIST\":\"default\","
                + "\"CGI\":\"y\",\"MAX_DEFER_FAIL_PERCENTAGE\":\"unlimited\",\"MAXSQL\":\"unlimited\",\"MAXPARK\":\"0\""
                + ",\"MAXADDON\":\"0\",\"HASSHELL\":\"n\",\"MAXLST\":\"unlimited\",\"_PACKAGE_EXTENSIONS\":\"\","
                + "\"LANG\":\"en\",\"MAXFTP\":\"unlimited\",\"MAXPOP\":\"unlimited\",\"BWLIMIT\":\"unlimited\","
                + "\"DIGESTAUTH\":\"n\",\"IP\":\"n\",\"CPMOD\":\"paper_lantern\",\"MAXSUB\":\"unlimited\"}]}}";
        when(cpClient.listPackages()).thenReturn(returnVal);
        List<String> packages = service.listPackages(hfsVmId);
        String [] expectedPackages = new String[] {"default", "test-package-1"};
        Assert.assertArrayEquals(expectedPackages, packages.toArray());
    }

    @Test(expected=CpanelTimeoutException.class)
    public void listPackagesParseException() throws Exception {
        String returnVal = "{'not-valid': 'json'}";
        when(cpClient.listPackages()).thenReturn(returnVal);
        service.listPackages(hfsVmId);
    }

    @Test
    public void listPackagesNoMetadata() throws Exception {
        String returnVal = "{\"data\":{\"blah\": \"foo\"}, \"metadata\": null}";
        when(cpClient.listPackages()).thenReturn(returnVal);

        try {
            service.listPackages(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM list package failed due to reason: No reason provided", e.getMessage());
        }
    }

    @Test
    public void listPackagesResultNotOk() throws Exception {
        String reason = "no-workie";
        String returnVal = "{\"metadata\":{\"version\":1,\"reason\":\"" + reason + "\", \"result\":0}}";
        when(cpClient.listPackages()).thenReturn(returnVal);

        try {
            service.listPackages(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("WHM list package failed due to reason: " + reason, e.getMessage());
        }
    }

    @Test
    public void listPackagesNullData() throws Exception {
        String returnVal = "{\"data\": null, \"metadata\":{\"version\":1,\"reason\":\"OK\", \"result\":1}}";
        when(cpClient.listPackages()).thenReturn(returnVal);

        try {
            service.listPackages(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("Error while handling response for call listPackages", e.getMessage());
        }
    }

    @Test
    public void listPackagesNullPackages() throws Exception {
        String returnVal = "{\"data\": {\"pkg\": null}, \"metadata\":{\"version\":1,\"reason\":\"OK\", \"result\":1}}";
        when(cpClient.listPackages()).thenReturn(returnVal);

        try {
            service.listPackages(hfsVmId);
            Assert.fail("This test shouldn't get here");
        }
        catch (RuntimeException e) {
            Assert.assertEquals("No cpanel packages present", e.getMessage());
        }
    }

    @Test
    public void updateNginxUsesCpanelClient() throws Exception{
        boolean enabled = true;
        String returnVal = "{\"metadata\":{\"command\":\"nginxmanager_set_cache_config\",\"version\":1," +
                "\"reason\":\"OK\",\"result\":1}}";
        List<String> usernames = Arrays.asList("vpsdev1", "vpsdev2");
        when(cpClient.updateNginx(enabled, usernames)).thenReturn(returnVal);

        service.updateNginx(hfsVmId, enabled, usernames);

        verify(cpClient, times(1))
                .updateNginx(enabledArgumentCaptor.capture(), usernamesArgumentCaptor.capture());
        Assert.assertEquals(enabled, enabledArgumentCaptor.getValue());
        Assert.assertEquals(usernames, usernamesArgumentCaptor.getValue());
    }

    @Test(expected = RuntimeException.class)
    public void updateNginxNoMetadata() throws Exception {
        boolean enabled = true;
        List<String> usernames = Arrays.asList("vpsdev1", "vpsdev2");
        String returnVal = "{\"metadata\": null}";
        when(cpClient.updateNginx(enabled, usernames)).thenReturn(returnVal);

        service.updateNginx(hfsVmId, enabled, usernames);
    }

    @Test(expected = RuntimeException.class)
    public void updateNginxResultNotOk() throws Exception {
        boolean enabled = true;
        List<String> usernames = Arrays.asList("vpsdev1", "vpsdev2");
        String reason = "no-workie";
        String returnVal = "{\"metadata\":{\"result\":0,\"reason\":\"" + reason + "\", " +
                "\"command\":\"nginxmanager_set_cache_config\",\"version\":1}}";
        when(cpClient.updateNginx(enabled, usernames)).thenReturn(returnVal);

        service.updateNginx(hfsVmId, enabled, usernames);
    }

    @Test
    public void clearNginxCacheUsesCpanelClient() throws Exception{
        String returnVal = "{\"metadata\":{\"reason\":\"OK\",\"version\":1," +
                "\"command\":\"nginxmanager_clear_cache\",\"result\":1}}";
        List<String> usernames = Arrays.asList("vpsdev1", "vpsdev2");
        when(cpClient.clearNginxCache(usernames)).thenReturn(returnVal);

        service.clearNginxCache(hfsVmId, usernames);

        verify(cpClient, times(1))
                .clearNginxCache(usernamesArgumentCaptor.capture());
        Assert.assertEquals(usernames, usernamesArgumentCaptor.getValue());
    }

    @Test(expected = RuntimeException.class)
    public void clearNginxCacheNoMetadata() throws Exception {
        List<String> usernames = Arrays.asList("vpsdev1", "vpsdev2");
        String returnVal = "{\"metadata\": null}";
        when(cpClient.clearNginxCache(usernames)).thenReturn(returnVal);

        service.clearNginxCache(hfsVmId, usernames);
    }

    @Test(expected = RuntimeException.class)
    public void clearNginxCacheResultNotOk() throws Exception {
        List<String> usernames = Arrays.asList("vpsdev1", "vpsdev2");
        String reason = "no-workie";
        String returnVal = "{\"metadata\":{\"result\":0,\"reason\":\"" + reason + "\",\"version\":1," +
                "\"command\":\"nginxmanager_clear_cache\"}}";
        when(cpClient.clearNginxCache(usernames)).thenReturn(returnVal);

        service.clearNginxCache(hfsVmId, usernames);
    }

    @Test
    public void getTweakSettingsUsesCpanelClient() throws Exception{
        String key = "allowremotedomains";
        String returnVal = "{\"data\":{\"tweaksetting\":{\"value\":\"1\",\"key\":\"allowremotedomains\"}}," +
                "\"metadata\":{\"version\":1,\"result\":1,\"command\":\"get_tweaksetting\",\"reason\":\"OK\"}}";
        when(cpClient.getTweakSettings(key)).thenReturn(returnVal);

        service.getTweakSettings(hfsVmId, key);

        verify(cpClient, times(1))
                .getTweakSettings(keyArgumentCaptor.capture());
        Assert.assertEquals(key, keyArgumentCaptor.getValue());
    }

    @Test(expected = RuntimeException.class)
    public void getTweakSettingsResultNotOk() throws Exception {
        String key = "allowremotedomains";
        String returnVal = "{\"metadata\":{\"command\":\"get_tweaksetting\"," +
                "\"reason\":\"Invalid tweaksetting key\",\"version\":1,\"result\":0}}";
        when(cpClient.getTweakSettings(key)).thenReturn(returnVal);

        service.getTweakSettings(hfsVmId, key);
    }

    @Test
    public void setTweakSettingsUsesCpanelClient() throws Exception{
        String key = "allowremotedomains";
        String value = "1";
        String returnVal = "{\"metadata\":{\"command\":\"set_tweaksetting\",\"reason\":\"OK\",\"version\":1,\"result\":1}}";
        when(cpClient.setTweakSettings(key, value)).thenReturn(returnVal);

        service.setTweakSettings(hfsVmId, key, value);

        verify(cpClient, times(1))
                .setTweakSettings(keyArgumentCaptor.capture(), valueArgumentCaptor.capture());
        Assert.assertEquals(key, keyArgumentCaptor.getValue());
    }

    @Test(expected = RuntimeException.class)
    public void setTweakSettingsResultNotOk() throws Exception {
        String key = "allowremotedomains";
        String value = "1";
        String returnVal = "{\"metadata\":{\"version\":1,\"result\":0,\"reason\":" +
                "\"Invalid tweaksetting key\",\"command\":\"set_tweaksetting\"}}";
        when(cpClient.setTweakSettings(key, value)).thenReturn(returnVal);

        service.setTweakSettings(hfsVmId, key, value);
    }
}
