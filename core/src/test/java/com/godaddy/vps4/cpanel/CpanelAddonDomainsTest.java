package com.godaddy.vps4.cpanel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

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

        UUID ipId = UUID.randomUUID();
        IpAddress ip = new IpAddress(1, ipId, "123.0.0.1", IpAddressType.PRIMARY, null, null, null);
        when(networkService.getVmPrimaryAddress(hfsVmId)).thenReturn(ip);

        when(cpanelAccessHashService.getAccessHash(eq(hfsVmId), eq("123.0.0.1"), any(), any())).thenReturn("randomaccesshash");

        service = new TestDefaultVps4CpanelService(cpanelAccessHashService, networkService, 10);
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




}
