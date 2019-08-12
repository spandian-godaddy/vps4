package com.godaddy.vps4.panopta;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.panopta.jdbc.JdbcPanoptaDataService;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class PanoptaDataServiceTest {

    private Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource = injector.getInstance(DataSource.class);
    private UUID orionGuid = UUID.randomUUID();
    private String fakeCustomerKey = "fake_customer_key";
    private String fakeServerKey = "totally-fake-server-key";

    private VirtualMachine vm;

    private PanoptaDataService panoptaDataService;
    private PanoptaCustomer panoptaCustomer;
    private PanoptaServer panoptaServer;

    @Before
    public void setUp() throws Exception {
        vm = SqlTestData.insertTestVm(orionGuid, dataSource);
        panoptaDataService = new JdbcPanoptaDataService(dataSource);
        String fakePartnerCustomerKey = "godaddy_totally-fake-uuid";
        panoptaCustomer = new PanoptaCustomer(fakeCustomerKey, fakePartnerCustomerKey);
        long fakeServerId = 1234567;
        panoptaServer = new PanoptaServer(fakePartnerCustomerKey, fakeServerId, fakeServerKey);
    }

    @After
    public void tearDown() throws Exception {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
    }

    @Test
    public void createPanoptaDetails() {
        panoptaDataService.createPanoptaDetails(vm.vmId, panoptaCustomer, panoptaServer);

        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vm.vmId);
        assertEquals(fakeCustomerKey, panoptaDetail.getCustomerKey());
        assertEquals(1234567, panoptaDetail.getServerId());
        assertEquals(fakeServerKey, panoptaDetail.getServerKey());
        assertNotNull(panoptaDetail.getCreated());
    }

    @Test
    public void removeServerInstanceFromPanopta() {
        panoptaDataService.createPanoptaDetails(vm.vmId, panoptaCustomer, panoptaServer);
        panoptaDataService.setServerDestroyedInPanopta(vm.vmId);

        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vm.vmId);
        assertNotNull(panoptaDetail.getDestroyed());
    }
}
