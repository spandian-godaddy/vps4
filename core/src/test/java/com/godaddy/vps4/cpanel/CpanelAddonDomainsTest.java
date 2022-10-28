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

public class CpanelAddonDomainsTest {

    Vps4CpanelService service;
    CpanelAccessHashService cpanelAccessHashService = mock(CpanelAccessHashService.class);
    NetworkService networkService = mock(NetworkService.class);
    Config config = mock(Config.class);
    CpanelClient cpClient = mock(CpanelClient.class);
    @Captor private ArgumentCaptor<String> domainArgumentCaptor;
    @Captor private ArgumentCaptor<String> usernameArgumentCaptor;
    @Captor private ArgumentCaptor<String> passwordArgumentCaptor;
    @Captor private ArgumentCaptor<String> planArgumentCaptor;
    @Captor private ArgumentCaptor<String> emailArgumentCaptor;

    long hfsVmId = 1234;

    public class TestDefaultVps4CpanelService extends DefaultVps4CpanelService {
        public TestDefaultVps4CpanelService(CpanelAccessHashService accessHashService,
                                            NetworkService networkService,
                                            int timeoutVal) {
            super(accessHashService, networkService, timeoutVal);
        }

        @Override
        protected CpanelClient getCpanelClient(String publicIp, String accessHash){
            return cpClient;
        }
    }

    @Before
    public void setupTest(){
        when(config.get("vps4.callable.timeout", "10000")).thenReturn("10");

        UUID vmId = UUID.randomUUID();
        IpAddress ip = new IpAddress(1, 1, vmId, "123.0.0.1", IpAddressType.PRIMARY, null, null, null, 4);
        when(networkService.getVmPrimaryAddress(hfsVmId)).thenReturn(ip);
        when(cpanelAccessHashService.getAccessHash(eq(hfsVmId), eq("123.0.0.1"), any())).thenReturn("randomaccesshash");

        service = new TestDefaultVps4CpanelService(cpanelAccessHashService, networkService, 10);
        MockitoAnnotations.initMocks(this);
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
}
