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
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.security.Vps4User;
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
    private final String fakeServerKey2 = "totally-fake-server-key-2";
    private final String fakeShopperId = "so-fake-shopperid";
    private final String fakePartnerCustomerKey = "gdtest_" + fakeShopperId;
    private final String fakeTemplateId = "12345";
    private final long fakeServerId = 1234567;
    private final long fakeServerId2 = 1234568;

    private VirtualMachine vm;
    private VirtualMachine vm2;

    private PanoptaDataService panoptaDataService;

    private PanoptaServer panoptaServer;
    private PanoptaServer panoptaServer2;
    private Vps4User user;
    private Config config = mock(Config.class);

    private Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource = injector.getInstance(DataSource.class);

    @Before
    public void setUp() throws Exception {
        user = SqlTestData.insertTestVps4User(dataSource);
        vm = SqlTestData.insertTestVm(orionGuid, dataSource, user.getId());
        vm2 = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, user.getId());
        panoptaDataService = new JdbcPanoptaDataService(dataSource, config);
        String fakeName = "s64-202-190-85.secureserver.net";
        String fakeFqdn = "s64-202-190-85.secureserver.net";
        String serverGroup = "https://api2.panopta.com/v2/server_group/348625";
        List<String> fakeAdditionalFqdns = Arrays.asList("thisfqdn.isdefinitely.fake");

        PanoptaServer.Status status = PanoptaServer.Status.ACTIVE;
        panoptaServer = new PanoptaServer(fakePartnerCustomerKey, fakeServerId, fakeServerKey, fakeName, fakeFqdn,
                                        fakeAdditionalFqdns, serverGroup, status, Instant.now());
        panoptaServer2 = new PanoptaServer(fakePartnerCustomerKey, fakeServerId2,
                fakeServerKey2, fakeName, fakeFqdn,
                fakeAdditionalFqdns, serverGroup, status, Instant.now());

        when(config.get("panopta.api.partner.customer.key.prefix")).thenReturn("gdtest_");
    }

    @After
    public void tearDown() {
        Sql.with(dataSource).exec("DELETE FROM panopta_additional_fqdns paf USING panopta_server ps WHERE ps.server_id = ? AND ps.id = paf.id", null, fakeServerId);
        Sql.with(dataSource).exec("DELETE FROM panopta_additional_fqdns paf USING panopta_server ps WHERE ps.server_id = ? AND ps.id = paf.id", null, fakeServerId2);
        Sql.with(dataSource).exec("DELETE FROM panopta_server WHERE vm_id = ?", null, vm.vmId);
        Sql.with(dataSource).exec("DELETE FROM panopta_server WHERE vm_id = ?", null, vm2.vmId);
        Sql.with(dataSource).exec("DELETE FROM panopta_customer WHERE partner_customer_key = ?", null, fakePartnerCustomerKey);

        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm2.vmId, dataSource);
        SqlTestData.deleteTestVps4User(dataSource);
    }

    @Test
    public void createPanoptaCustomer() {
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);

        PanoptaCustomerDetails panoptaCustomerDetails = panoptaDataService.getPanoptaCustomerDetails(fakeShopperId);
        assertNotNull(panoptaCustomerDetails);
        assertEquals(fakePartnerCustomerKey, panoptaCustomerDetails.getPartnerCustomerKey());
        assertEquals(fakeCustomerKey, panoptaCustomerDetails.getCustomerKey());
        assertNotNull(panoptaCustomerDetails.getCreated());
        assertFalse(panoptaCustomerDetails.getDestroyed().isBefore(Instant.now()));
    }

    @Test
    public void updatePanoptaCustomerIfExists() {
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);

        PanoptaCustomerDetails panoptaCustomerDetails = panoptaDataService.getPanoptaCustomerDetails(fakeShopperId);
        assertNotNull(panoptaCustomerDetails);
        assertEquals(fakePartnerCustomerKey, panoptaCustomerDetails.getPartnerCustomerKey());
        assertEquals(fakeCustomerKey, panoptaCustomerDetails.getCustomerKey());
        assertNotNull(panoptaCustomerDetails.getCreated());
        assertFalse(panoptaCustomerDetails.getDestroyed().isBefore(Instant.now()));
    }

    @Test
    public void createOrUpdatePanoptaCustomerFromKeyDoesNotAddExtraPrefix() {
        panoptaDataService.createOrUpdatePanoptaCustomerFromKey(fakePartnerCustomerKey, fakeCustomerKey);

        PanoptaCustomerDetails panoptaCustomerDetails = panoptaDataService.getPanoptaCustomerDetails(fakeShopperId);
        assertNotNull(panoptaCustomerDetails);
        assertEquals(fakePartnerCustomerKey, panoptaCustomerDetails.getPartnerCustomerKey());
        assertEquals(fakePartnerCustomerKey.split("gdtest_").length - 1, 1);
        assertEquals(fakeCustomerKey, panoptaCustomerDetails.getCustomerKey());
        assertNotNull(panoptaCustomerDetails.getCreated());
        assertFalse(panoptaCustomerDetails.getDestroyed().isBefore(Instant.now()));
    }

    @Test
    public void destroyPanoptaServer() {
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
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
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
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
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);

        List<PanoptaServerDetails> panoptaServerDetailsList = panoptaDataService.getPanoptaServerDetailsList(fakeShopperId);
        assertNotNull(panoptaServerDetailsList);
        assertFalse(panoptaServerDetailsList.isEmpty());
    }

    @Test
    public void removeAllActivePanoptaServersOfCustomer() {
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);
        panoptaDataService.createPanoptaServer(vm2.vmId, fakeShopperId, fakeTemplateId, panoptaServer2);

        panoptaDataService.setAllPanoptaServersOfCustomerDestroyed(fakeShopperId);

        PanoptaServerDetails panoptaServerDetails = panoptaDataService.getPanoptaServerDetails(vm.vmId);
        assertNull(panoptaServerDetails);
        PanoptaServerDetails panoptaServerDetails2 = panoptaDataService.getPanoptaServerDetails(vm2.vmId);
        assertNull(panoptaServerDetails2);
        PanoptaCustomerDetails panoptaCustomerDetails = panoptaDataService.getPanoptaCustomerDetails(fakeShopperId);
        assertNotNull(panoptaCustomerDetails);
        assertEquals(fakePartnerCustomerKey, panoptaCustomerDetails.getPartnerCustomerKey());
        assertEquals(fakeCustomerKey, panoptaCustomerDetails.getCustomerKey());
    }

    @Test
    public void removePanoptaCustomer() {
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
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
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);

        boolean wasDestroyed = panoptaDataService.checkAndSetPanoptaCustomerDestroyed(fakeShopperId);
        assertFalse(wasDestroyed);
        List<PanoptaServerDetails> panoptaServerDetailsList = panoptaDataService.getPanoptaServerDetailsList(fakeShopperId);
        assertFalse(panoptaServerDetailsList.isEmpty());
        assertEquals(1, panoptaServerDetailsList.size());
    }

    @Test
    public void getPanoptaDetail() {
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
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
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);

        UUID vmId = panoptaDataService.getVmId(fakeServerKey);
        assertEquals(vm.vmId, vmId);
    }

    @Test
    public void canAddAndGetPanoptaAdditionalFqdns() {
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);
        panoptaDataService.addPanoptaAdditionalFqdn("fqdn.fake", panoptaServer.serverId);
        List<String> additionalFqdns = panoptaDataService.getPanoptaActiveAdditionalFqdns(vm.vmId);

        assertEquals(additionalFqdns.get(0), "fqdn.fake");
    }

    @Test
    public void canAddAndDeletePanoptaAdditionalFqdns() {
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
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
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);
        panoptaDataService.addPanoptaAdditionalFqdn("fqdn.fake", panoptaServer.serverId);
        Boolean fqdnExists = panoptaDataService.activeAdditionalFqdnExistsForServer("fqdn.fake", panoptaServer.serverId);
        assertEquals(fqdnExists, true);

        panoptaDataService.deletePanoptaAdditionalFqdn("fqdn.fake", panoptaServer.serverId);
        Boolean deletedFqdnExists = panoptaDataService.activeAdditionalFqdnExistsForServer("fqdn.fake", panoptaServer.serverId);
        assertEquals(false, deletedFqdnExists);
    }

    @Test
    public void canDeletePanoptaAdditionalFqdnsFromVmId() {
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);
        panoptaDataService.addPanoptaAdditionalFqdn("fqdn.fake", panoptaServer.serverId);
        panoptaDataService.addPanoptaAdditionalFqdn("fqdn3.fake", panoptaServer.serverId);
        List<String> additionalFqdns = panoptaDataService.getPanoptaActiveAdditionalFqdns(vm.vmId);
        assertEquals("fqdn.fake", additionalFqdns.get(0));
        assertEquals("fqdn3.fake", additionalFqdns.get(1));

        panoptaDataService.deleteVirtualMachineAdditionalFqdns(vm.vmId);
        List<String> additionalFqdnsDeleted = panoptaDataService.getPanoptaActiveAdditionalFqdns(vm.vmId);
        assertEquals(0, additionalFqdnsDeleted.size());
    }

    @Test
    public void canGetLowerCaseFqdnAndValidOnByVmId() {
        panoptaDataService.createOrUpdatePanoptaCustomer(fakeShopperId, fakeCustomerKey);
        panoptaDataService.createPanoptaServer(vm.vmId, fakeShopperId, fakeTemplateId, panoptaServer);
        panoptaDataService.addPanoptaAdditionalFqdn("CapitalizedFqdn.fake", panoptaServer.serverId);

        Map<String, Instant> map = panoptaDataService.getPanoptaAdditionalFqdnWithValidOn(vm.vmId);
        assertFalse(map.get("capitalizedfqdn.fake").isBefore(Instant.now().minusSeconds(1)));
    }
}
