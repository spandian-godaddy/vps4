package com.godaddy.vps4.panopta;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.panopta.jdbc.JdbcPanoptaDataService;
import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class PanoptaDataServiceTest {

    private final UUID orionGuid = UUID.randomUUID();
    private final String fakeCustomerKey = "fake_customer_key";
    private final String fakeServerKey = "totally-fake-server-key";
    private final String fakeShopperId = "so-fake-shopperid";
    private final String fakePartnerCustomerKey = "gdtest_" + fakeShopperId;
    private final String fakeTemplateId = "12345";
    private final long fakeServerId = 1234567;

    private VirtualMachine vm;

    private PanoptaDataService panoptaDataService;
    private PanoptaServer panoptaServer;
    private Config config = mock(Config.class);

    private Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource = injector.getInstance(DataSource.class);

    @Before
    public void setUp() throws Exception {
        vm = SqlTestData.insertTestVm(orionGuid, dataSource);
        panoptaDataService = new JdbcPanoptaDataService(dataSource, config);
        String fakeName = "s64-202-190-85.secureserver.net";
        String fakeFqdn = "s64-202-190-85.secureserver.net";
        String serverGroup = "https://api2.panopta.com/v2/server_group/348625";
        List<String> fakeAdditionalFqdns = Arrays.asList("thisfqdn.isdefinitely.fake");

        PanoptaServer.Status status = PanoptaServer.Status.ACTIVE;
        panoptaServer = new PanoptaServer(fakePartnerCustomerKey, fakeServerId, fakeServerKey, fakeName, fakeFqdn,
                                        fakeAdditionalFqdns, serverGroup, status, Instant.now());
        when(config.get("panopta.api.partner.customer.key.prefix")).thenReturn("gdtest_");
    }

    @After
    public void tearDown() {
        Sql.with(dataSource).exec("DELETE FROM panopta_additional_fqdns WHERE server_id = ?", null, fakeServerId);
        Sql.with(dataSource).exec("DELETE FROM panopta_server WHERE vm_id = ?", null, vm.vmId);
        Sql.with(dataSource).exec("DELETE FROM panopta_customer WHERE partner_customer_key = ?", null, fakePartnerCustomerKey);

        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
    }

    @Test
    public void createPanoptaCustomer() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);

        PanoptaCustomerDetails panoptaCustomerDetails = panoptaDataService.getPanoptaCustomerDetails(fakeShopperId);
        assertNotNull(panoptaCustomerDetails);
        assertEquals(fakePartnerCustomerKey, panoptaCustomerDetails.getPartnerCustomerKey());
        assertEquals(fakeCustomerKey, panoptaCustomerDetails.getCustomerKey());
        assertNotNull(panoptaCustomerDetails.getCreated());
        assertFalse(panoptaCustomerDetails.getDestroyed().isBefore(Instant.now()));
    }

    @Test
    public void updatePanoptaCustomerIfExists() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);

        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        PanoptaCustomerDetails panoptaCustomerDetails = panoptaDataService.getPanoptaCustomerDetails(fakeShopperId);
        assertNotNull(panoptaCustomerDetails);
        assertEquals(fakePartnerCustomerKey, panoptaCustomerDetails.getPartnerCustomerKey());
        assertEquals(fakeCustomerKey, panoptaCustomerDetails.getCustomerKey());
        assertNotNull(panoptaCustomerDetails.getCreated());
        assertFalse(panoptaCustomerDetails.getDestroyed().isBefore(Instant.now()));
    }

    @Test
    public void destroyPanoptaServer() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);

        panoptaDataService.setPanoptaServerDestroyed(vm.vmId);

        PanoptaServerDetails panoptaServerDetails = panoptaDataService.getPanoptaServerDetails(vm.vmId);
        assertNull(panoptaServerDetails);
        PanoptaCustomerDetails panoptaCustomerDetails = panoptaDataService.getPanoptaCustomerDetails(fakeShopperId);
        assertNotNull(panoptaCustomerDetails);
        assertEquals(fakePartnerCustomerKey, panoptaCustomerDetails.getPartnerCustomerKey());
        assertEquals(fakeCustomerKey, panoptaCustomerDetails.getCustomerKey());
    }

    @Test
    public void createPanoptaServer() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);

        PanoptaServerDetails panoptaServerDetails = panoptaDataService.getPanoptaServerDetails(vm.vmId);
        assertNotNull(panoptaServerDetails);
        assertEquals(fakePartnerCustomerKey, panoptaServerDetails.getPartnerCustomerKey());
        assertEquals(fakeServerKey, panoptaServerDetails.getServerKey());
        assertEquals(fakeServerId, panoptaServerDetails.getServerId());
        assertTrue(panoptaServerDetails.getCreated().isBefore(Instant.now()));
        assertFalse(panoptaServerDetails.getDestroyed().isBefore(Instant.now()));
    }

    @Test
    public void getActivePanoptaServers() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);

        List<PanoptaServerDetails> panoptaServerDetailsList = panoptaDataService.getPanoptaServerDetailsList(fakeShopperId);
        assertNotNull(panoptaServerDetailsList);
        assertFalse(panoptaServerDetailsList.isEmpty());
    }

    @Test
    public void removePanoptaCustomer() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);
        panoptaDataService.setPanoptaServerDestroyed(vm.vmId);

        boolean wasDestroyed = panoptaDataService.checkAndSetPanoptaCustomerDestroyed(fakeShopperId);
        assertTrue(wasDestroyed);
        List<PanoptaServerDetails> panoptaServerDetailsList = panoptaDataService.getPanoptaServerDetailsList(fakeShopperId);
        assertEquals(0, panoptaServerDetailsList.size());
        assertNull(panoptaDataService.getPanoptaCustomerDetails(fakeShopperId));
    }

    @Test
    public void doesNotRemoveCustomerIfActiveServersExist() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);

        boolean wasDestroyed = panoptaDataService.checkAndSetPanoptaCustomerDestroyed(fakeShopperId);
        assertFalse(wasDestroyed);
        List<PanoptaServerDetails> panoptaServerDetailsList = panoptaDataService.getPanoptaServerDetailsList(fakeShopperId);
        assertFalse(panoptaServerDetailsList.isEmpty());
        assertEquals(1, panoptaServerDetailsList.size());
    }

    @Test
    public void getPanoptaDetail() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);

        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vm.vmId);
        assertNotNull(panoptaDetail);
        assertEquals(fakePartnerCustomerKey, panoptaDetail.getPartnerCustomerKey());
        assertEquals(fakeCustomerKey, panoptaDetail.getCustomerKey());
        assertEquals(fakeServerKey, panoptaDetail.getServerKey());
        assertEquals(fakeServerId, panoptaDetail.getServerId());
        assertEquals(vm.vmId, panoptaDetail.getVmId());
    }

    @Test
    public void canGetVmIdByServerKey() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);

        UUID vmId = panoptaDataService.getVmId(fakeServerKey);
        assertEquals(vm.vmId, vmId);
    }

    @Test
    public void canAddAndGetPanoptaAdditionalFqdns() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);
        panoptaDataService.addPanoptaAdditionalFqdn("fqdn.fake", panoptaServer.serverId);
        List<String> additionalFqdns = panoptaDataService.getPanoptaActiveAdditionalFqdns(vm.vmId);

        assertEquals(additionalFqdns.get(0), "fqdn.fake");
    }

    @Test
    public void canAddAndDeletePanoptaAdditionalFqdns() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);
        panoptaDataService.addPanoptaAdditionalFqdn("fqdn.fake", panoptaServer.serverId);
        List<String> additionalFqdns = panoptaDataService.getPanoptaActiveAdditionalFqdns(vm.vmId);
        assertEquals("fqdn.fake", additionalFqdns.get(0));

        panoptaDataService.deletePanoptaAdditionalFqdn("fqdn.fake", panoptaServer.serverId);
        List<String> additionalFqdnsDeleted = panoptaDataService.getPanoptaActiveAdditionalFqdns(vm.vmId);
        assertEquals(0, additionalFqdnsDeleted.size());
    }

    @Test
    public void canCheckIfActivePanoptaAdditionalFqdnExists() {
        panoptaDataService.createPanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);
        panoptaDataService.addPanoptaAdditionalFqdn("fqdn.fake", panoptaServer.serverId);
        Boolean fqdnExists = panoptaDataService.activeAdditionalFqdnExistsForServer("fqdn.fake", panoptaServer.serverId);
        assertEquals(fqdnExists, true);

        panoptaDataService.deletePanoptaAdditionalFqdn("fqdn.fake", panoptaServer.serverId);
        Boolean deletedFqdnExists = panoptaDataService.activeAdditionalFqdnExistsForServer("fqdn.fake", panoptaServer.serverId);
        assertEquals(false, deletedFqdnExists);
    }
}
