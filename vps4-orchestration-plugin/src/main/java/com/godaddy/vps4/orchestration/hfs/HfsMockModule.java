package com.godaddy.vps4.orchestration.hfs;


import static org.mockito.Mockito.doNothing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;

import com.google.inject.Provides;
import gdg.hfs.request.CompleteResponse;
import gdg.hfs.vhfs.ecomm.Account;
import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.ecomm.MetadataUpdate;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.mailrelay.MailRelay;
import gdg.hfs.vhfs.mailrelay.MailRelayHistory;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import gdg.hfs.vhfs.vm.CreateVMRequest;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import gdg.hfs.vhfs.snapshot.SnapshotService;
import gdg.hfs.vhfs.snapshot.Snapshot;
import gdg.hfs.vhfs.snapshot.SnapshotAction;

import javax.ws.rs.NotFoundException;

public class HfsMockModule extends AbstractModule {

    private static final Map<Long, Snapshot> customerSnapshots;
    private static final Map<Long, List<SnapshotAction>> snapshotActionList;
    private static final Map<Long, SnapshotActionEntry> snapshotActions;
    private static final Map<String, Account> ecommAccounts;
    private static final Map<String, List<Account>> shopperECommAccounts;

    static {
        customerSnapshots = new HashMap<>();
        snapshotActionList = new HashMap<>();
        snapshotActions = new HashMap<>();
        ecommAccounts = new HashMap<>();
        shopperECommAccounts = new HashMap<>();
    }

    @Override
    public void configure() {
        NetworkService netService = buildMockNetworkService();
        bind(NetworkService.class).toInstance(netService);
        VmService vmService = buildMockVmService();
        bind(VmService.class).toInstance(vmService);
        SysAdminService sysAdminService = buildSysAdminService();
        bind(SysAdminService.class).toInstance(sysAdminService);
        CPanelService cpService = Mockito.mock(CPanelService.class);
        bind(CPanelService.class).toInstance(cpService);
        MailRelayService mailRelayService = buildMailRelayService();
        bind(MailRelayService.class).toInstance(mailRelayService);
        PleskService pleskService = buildPleskService();
        bind(PleskService.class).toInstance(pleskService);
        NodePingService nodePingService = buildNodePingService();
        bind(NodePingService.class).toInstance(nodePingService);

    }

    private PleskService buildPleskService() {
        PleskAction completeAction = new PleskAction();
        completeAction.status = PleskAction.Status.COMPLETE;
        PleskService pleskService = Mockito.mock(PleskService.class);
        Mockito.when(pleskService.imageConfig(Mockito.anyLong(),  Mockito.anyString(), Mockito.anyString())).thenReturn(completeAction);
        Mockito.when(pleskService.adminPassUpdate(Mockito.anyLong(), Mockito.anyString())).thenReturn(completeAction);
        return pleskService;
    }

    private MailRelayService buildMailRelayService() {
        MailRelayService mailRelayService = Mockito.mock(MailRelayService.class);

        MailRelay mailRelayTarget = new MailRelay();
        mailRelayTarget.quota = 5000;
        
        MailRelayHistory singleHistory = new MailRelayHistory();
        List<MailRelayHistory> history = new ArrayList<>();
        history.add(singleHistory);

        Mockito.when(mailRelayService.setRelayQuota(Mockito.anyString(), Mockito.anyObject())).thenReturn(mailRelayTarget);
        Mockito.when(mailRelayService.getMailRelay(Mockito.anyString())).thenReturn(mailRelayTarget);
        Mockito.when(mailRelayService.getRelayHistory(Mockito.anyString())).thenReturn(history);

        return mailRelayService;
    }

    private SysAdminService buildSysAdminService(){
       SysAdminService sysAdminService = Mockito.mock(SysAdminService.class);
       SysAdminAction completeAction = new SysAdminAction();
       completeAction.status = SysAdminAction.Status.COMPLETE;

       SysAdminAction errorAction = new SysAdminAction();
       errorAction.status = SysAdminAction.Status.FAILED;

       Mockito.when(sysAdminService.enableAdmin(Mockito.anyLong(), Mockito.anyString())).thenReturn(completeAction);
       Mockito.when(sysAdminService.disableAdmin(Mockito.anyLong(), Mockito.anyString())).thenReturn(completeAction);
       Mockito.when(sysAdminService.changePassword(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString())).thenReturn(completeAction);
       Mockito.when(sysAdminService.changeHostname(Mockito.anyLong(), Mockito.anyString(), Mockito.any())).thenReturn(completeAction);
       Mockito.when(sysAdminService.getSysAdminAction(Mockito.anyLong())).thenReturn(completeAction);
        Mockito.when(sysAdminService.configureMTA(Mockito.anyLong(), Mockito.anyString())).thenReturn(completeAction);

       return sysAdminService;

    }


    private VmService buildMockVmService() {
        VmService vmService = Mockito.mock(VmService.class);
        Vm vm0 = new Vm();
        vm0.vmId = 0;
        vm0.status = "Live";
        Mockito.when(vmService.getVm(0)).thenReturn(vm0);
        VmAction completeDelAction = new VmAction();
        completeDelAction.vmActionId = 1111;
        completeDelAction.vmId = 0;
        completeDelAction.state = VmAction.Status.COMPLETE;
        completeDelAction.tickNum = 1;
        Mockito.when(vmService.createVmWithFlavor(Mockito.any(CreateVMWithFlavorRequest.class))).thenReturn(completeDelAction);
        Mockito.when(vmService.getVmAction(Mockito.anyLong(),Mockito.anyLong())).thenReturn(completeDelAction);
        Mockito.when(vmService.destroyVm(0)).thenReturn(completeDelAction);
        Mockito.when(vmService.createVm(Mockito.any(CreateVMRequest.class))).thenReturn(completeDelAction);
        Mockito.when(vmService.startVm(Mockito.anyLong())).thenReturn(completeDelAction);
        Mockito.when(vmService.stopVm(Mockito.anyLong())).thenReturn(completeDelAction);
        Mockito.when(vmService.createVmWithFlavor(Mockito.any(CreateVMWithFlavorRequest.class))).thenReturn(completeDelAction);
        Mockito.when(vmService.getVmAction(Mockito.anyLong(), Mockito.anyLong())).thenReturn(completeDelAction);
        return vmService;
    }


    private NetworkService buildMockNetworkService() {
        Answer<AddressAction> answer = new Answer<AddressAction>() {
            // returns 3 in progress responses then a complete response
            private int timesCalled = 0;

            public AddressAction answer(InvocationOnMock invocation) throws Throwable {
                if (timesCalled < 3) {
                    timesCalled++;
                    return buildAddressAction(AddressAction.Status.IN_PROGRESS);
                }
                timesCalled = 0;
                return buildAddressAction(AddressAction.Status.COMPLETE);
            }
        };

        NetworkService netService = Mockito.mock(NetworkService.class);
        AddressAction newAction = buildAddressAction(AddressAction.Status.NEW);
        Mockito.when(netService.unbindIp(0)).thenReturn(newAction);
        Mockito.when(netService.releaseIp(0)).thenReturn(newAction);
        Mockito.when(netService.acquireIp(Mockito.anyString(), Mockito.anyString())).thenReturn(newAction);
        Mockito.when(netService.bindIp(12345,0)).thenReturn(newAction);
        Mockito.when(netService.getAddressAction(12345, 54321)).thenAnswer(answer);
        Mockito.when(netService.getAddress(12345)).thenReturn(buildIpAddress());
        return netService;
    }

    private IpAddress buildIpAddress(){
        IpAddress ip = new IpAddress();
        ip.address = "2.23.123.111";
        ip.status = IpAddress.Status.UNBOUND;
        ip.addressId = 12345;
        return ip;
    }

    private AddressAction buildAddressAction(AddressAction.Status status) {
        AddressAction newAction = new AddressAction();
        newAction.status = status;
        newAction.addressId = 12345;
        newAction.addressActionId = 54321;
        return newAction;
    }

    private NodePingService buildNodePingService() {
        NodePingService nodePingService = Mockito.mock(NodePingService.class);

        NodePingCheck check = new NodePingCheck();
        check.accountId = 1234;
        check.checkId = 4321;
        check.nodepingCheckId = "fakeCheck";

        Mockito.when(nodePingService.createCheck(Mockito.anyLong(), Mockito.anyObject())).thenReturn(check);
        doNothing().when(nodePingService).deleteCheck(Mockito.anyLong(), Mockito.anyLong());
        Mockito.when(nodePingService.getCheck(Mockito.anyLong(), Mockito.anyLong())).thenReturn(check);
        return nodePingService;
    }

    private class SnapshotActionEntry {
        public long accessCount;
        public SnapshotAction action;

        public SnapshotActionEntry(SnapshotAction action) {
            this.action = action;
            this.accessCount = 0;
        }
    }

    @Provides
    public SnapshotService provideMockSnapshotService() {
        return new SnapshotService() {
            private Snapshot createSnapshotHelper(String name, String version) {
                long snapshotId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
                return new Snapshot(snapshotId, "mock-sgid", name,
                        version, "mock-imageId", "1234",
                        "5678", null);

            }

            private SnapshotAction createSnapshotAction(
                    long snapshotId, long vmId, int actionType, SnapshotAction.Status status) {
                SnapshotAction snapshotAction = new SnapshotAction();
                snapshotAction.actionId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
                snapshotAction.snapshotId = snapshotId;
                snapshotAction.actionType = actionType;
                snapshotAction.status = status;
                snapshotAction.vmId = vmId;
                return snapshotAction;
            }

            private void storeSnapshotAction(long snapshotId, SnapshotAction snapshotAction) {
                if (!snapshotActionList.containsKey(snapshotId)) {
                    snapshotActionList.put(snapshotId, new ArrayList<>());
                }

                List<SnapshotAction> actions = snapshotActionList.get(snapshotId);
                actions.add(snapshotAction);

                snapshotActions.put(snapshotAction.actionId, new SnapshotActionEntry(snapshotAction));
            }

            private SnapshotAction createAndStoreSnapshotAction(
                    long snapshotId, long vmId, int actionType, SnapshotAction.Status status) {
                SnapshotAction snapshotAction = this.createSnapshotAction(
                        snapshotId, vmId, actionType, status);
                this.storeSnapshotAction(snapshotId, snapshotAction);
                return snapshotAction;
            }

            private Boolean isSnapshotDestroyed(Long snapshotId) {
                return snapshotActionList.get(snapshotId).stream().anyMatch(sa -> sa.actionType == 2);
            }

            @Override
            public SnapshotAction createSnapshot(
                    long vmId, String name, String version, boolean whileRunning, boolean clean) {
                Snapshot snapshot = this.createSnapshotHelper(name, version);
                customerSnapshots.put(snapshot.snapshotId, snapshot);

                // actionType '1' is CREATE
                return this.createAndStoreSnapshotAction(
                        snapshot.snapshotId, vmId, 1, SnapshotAction.Status.NEW);
            }

            @Override
            public SnapshotAction destroySnapshot(long snapshotId) {
                if (!customerSnapshots.containsKey(snapshotId)
                        || !snapshotActionList.containsKey(snapshotId)
                        || this.isSnapshotDestroyed(snapshotId)) {
                    throw new NotFoundException("Snapshot not present or has already been destroyed");
                }

                // actionType '2' is DELETE
                // vmId is being passed as 123
                return this.createAndStoreSnapshotAction(
                        snapshotId, 123, 2, SnapshotAction.Status.COMPLETE);
            }

            @Override
            public SnapshotAction publishSnapshot(long snapshotId) {
                if (!customerSnapshots.containsKey(snapshotId)
                        || !snapshotActionList.containsKey(snapshotId)
                        || this.isSnapshotDestroyed(snapshotId)) {
                    throw new NotFoundException("Snapshot not present or has already been destroyed");
                }

                // actionType '3' is PUBLISH
                // vmId is being passed as 123
                return this.createAndStoreSnapshotAction(
                        snapshotId, 123, 3, SnapshotAction.Status.COMPLETE);
            }

            @Override
            public SnapshotAction getSnapshotAction(long snapshotActionId) {
                if (!snapshotActions.containsKey(snapshotActionId)) {
                    throw new NotFoundException("Snapshot action not present");
                }

                SnapshotActionEntry entry = snapshotActions.get(snapshotActionId);
                entry.accessCount++;
                // Mark the action as complete after 3 tries
                entry.action.status =  entry.accessCount > 3
                        ? SnapshotAction.Status.COMPLETE
                        : SnapshotAction.Status.IN_PROGRESS;

                return entry.action;
            }

            @Override
            public List<SnapshotAction> getSnapshotActionsBySgID(String sgid) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public List<SnapshotAction> getSnapshotActions(long snapshotId) {
                if (!snapshotActionList.containsKey(snapshotId)) {
                    throw new NotFoundException("Snapshot not present");
                }

                return snapshotActionList.get(snapshotId);
            }

            @Override
            public Snapshot getSnapshot(long snapshotId) {
                if (!customerSnapshots.containsKey(snapshotId)) {
                    throw new NotFoundException("Snapshot not present");
                }

                return customerSnapshots.get(snapshotId);
            }

            @Override
            public List<Snapshot> getSnapshots(String sgid) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public void onComplete(CompleteResponse completeResponse) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }
        };
    }

    @Provides
    public ECommService provideMockECommService() {
        return new ECommService() {
            @Override
            public List<Account> getAccounts(String shopperId) {
                return shopperECommAccounts.getOrDefault(shopperId, new ArrayList<>());
            }

            @Override
            public Account createAccount(Account account) {
                if (!ecommAccounts.containsKey(account.account_guid)) {
                    account.product_meta = new HashMap<>();
                    ecommAccounts.put(account.account_guid, account);

                    if (!shopperECommAccounts.containsKey(account.shopper_id)) {
                        shopperECommAccounts.put(account.shopper_id, new ArrayList<>());
                    }
                    List<Account> shopperAccounts = shopperECommAccounts.get(account.shopper_id);
                    shopperAccounts.add(account);
                }

                return account;
            }

            @Override
            public Account getAccount(String accountGuid) {
                return ecommAccounts.get(accountGuid);
            }

            @Override
            public Account updateAccount(String accountGuid, Account account) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public Map<String, String> updateProductMetadata(String accountGuid, MetadataUpdate metadataUpdate) {
                if (ecommAccounts.containsKey(accountGuid)) {
                    Account account = ecommAccounts.get(accountGuid);
                    if (account.product_meta.equals(metadataUpdate.from)) {
                        account.product_meta = metadataUpdate.to;
                        return account.product_meta;
                    }
                }

                return null;
            }
        };
    }
}