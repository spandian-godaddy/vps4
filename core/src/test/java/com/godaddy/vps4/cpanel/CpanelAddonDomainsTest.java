package com.godaddy.vps4.cpanel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
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
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

public class CpanelAddonDomainsTest {

    Vps4CpanelService service;
    CpanelAccessHashService cpanelAccessHashService = mock(CpanelAccessHashService.class);
    NetworkService networkService = mock(NetworkService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
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
                                            VirtualMachineService virtualMachineService,
                                            int timeoutVal) {
            super(accessHashService, networkService, virtualMachineService, timeoutVal);
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
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        virtualMachine.validOn = Instant.MAX;
        virtualMachine.primaryIpAddress = ip;
        when(networkService.getVmPrimaryAddress(hfsVmId)).thenReturn(ip);
        when(virtualMachineService.getVirtualMachine(hfsVmId)).thenReturn(virtualMachine);
        when(cpanelAccessHashService.getAccessHash(eq(hfsVmId), eq("123.0.0.1"), any())).thenReturn("randomaccesshash");

        service = new TestDefaultVps4CpanelService(cpanelAccessHashService, networkService, virtualMachineService, 10);
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
    public void listPackagesUsesCpanelClient() throws Exception{
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
