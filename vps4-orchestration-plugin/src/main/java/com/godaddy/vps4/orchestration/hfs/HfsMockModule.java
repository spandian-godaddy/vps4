package com.godaddy.vps4.orchestration.hfs;


import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.mockito.Mockito;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayHistory;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.mailrelay.MailRelayUpdate;
import com.godaddy.hfs.vm.Console;
import com.godaddy.hfs.vm.CreateVMRequest;
import com.godaddy.hfs.vm.CreateVMWithFlavorRequest;
import com.godaddy.hfs.vm.FlavorList;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmAddress;
import com.godaddy.hfs.vm.VmList;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.messaging.DefaultVps4MessagingService;
import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.messaging.models.Message;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import gdg.hfs.request.CompleteResponse;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelLicense;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.ecomm.Account;
import gdg.hfs.vhfs.ecomm.ECommDataCache;
import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.ecomm.MetadataUpdate;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.AddressActionList;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.IpAddressList;
import gdg.hfs.vhfs.network.NetworkServiceV2;
import gdg.hfs.vhfs.nodeping.CreateCheckRequest;
import gdg.hfs.vhfs.nodeping.NodePingAccount;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingEvent;
import gdg.hfs.vhfs.nodeping.NodePingNotification;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.nodeping.NodePingUptimeRecord;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.snapshot.Snapshot;
import gdg.hfs.vhfs.snapshot.SnapshotAction;
import gdg.hfs.vhfs.snapshot.SnapshotService;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminInstallable;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class HfsMockModule extends AbstractModule {

    private static final Map<Long, Snapshot> customerSnapshots;
    private static final Map<Long, List<SnapshotAction>> snapshotActionList;
    private static final Map<Long, SnapshotActionEntry> snapshotActions;
    private static final Map<String, Account> eCommAccounts;
    private static final Map<String, List<Account>> shopperECommAccounts;
    private static final Map<Long, Vm> customerVms;
    private static final Map<Long, List<VmAction>> vmActionList;
    private static final Map<Long, VmActionEntry> vmActions;
    private static final Map<Long, SysAdminAction> sysAdminActions;
    private static final Map<Long, List<SysAdminAction>> sysAdminActionList;
    private static final Map<String , MailRelay> mailRelays;
    private static final Map<Long, NodePingCheck> nodePingChecks;
    private static final Map<Long, List<NodePingCheck>> accountNodePingChecksList;
    private static final Map<Long, IpAddress> customerAddresses;
    private static final Map<Long, List<AddressAction>> addressActionList;
    private static final Map<Long, AddressActionEntry> addressActions;
    private static final Map<Long, CPanelAction> cPanelActions;
    private static final Map<String, Message> gdMessagingServiceMessages;

    static {
        customerSnapshots = new HashMap<>();
        snapshotActionList = new HashMap<>();
        snapshotActions = new HashMap<>();
        eCommAccounts = new HashMap<>();
        shopperECommAccounts = new HashMap<>();
        customerVms = new HashMap<>();
        vmActionList = new HashMap<>();
        vmActions = new HashMap<>();
        sysAdminActions = new HashMap<>();
        sysAdminActionList = new HashMap<>();
        mailRelays = new HashMap<>();
        nodePingChecks = new HashMap<>();
        accountNodePingChecksList = new HashMap<>();
        customerAddresses = new HashMap<>();
        addressActionList = new HashMap<>();
        addressActions = new HashMap<>();
        cPanelActions = new HashMap<>();
        gdMessagingServiceMessages = new HashMap<>();
    }

    @Override
    public void configure() {
        PleskService pleskService = buildPleskService();
        bind(PleskService.class).toInstance(pleskService);
    }

    private PleskService buildPleskService() {
        PleskAction completeAction = new PleskAction();
        completeAction.status = PleskAction.Status.COMPLETE;
        PleskService pleskService = Mockito.mock(PleskService.class);
        Mockito.when(pleskService.imageConfig(Mockito.anyLong(),  Mockito.anyString(), Mockito.anyString())).thenReturn(completeAction);
        Mockito.when(pleskService.adminPassUpdate(Mockito.anyLong(), Mockito.anyString())).thenReturn(completeAction);
        return pleskService;
    }

    @Provides
    public CPanelService provideCPanelService() {
        return new CPanelService() {
            private CPanelAction createCPanelAction(long vmId,
                                                        CPanelAction.ActionType actionType,
                                                        CPanelAction.Status status) {
                CPanelAction cPanelAction = new CPanelAction();
                cPanelAction.actionId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
                cPanelAction.vmId = vmId;
                cPanelAction.serverId = vmId;
                cPanelAction.actionType = actionType.id;
                cPanelAction.status = status;
                return cPanelAction;
            }

            private void storeCPanelAction(CPanelAction cPanelAction) {
                cPanelActions.put(cPanelAction.actionId, cPanelAction);
            }

            private CPanelAction createAndStoreCPanelAction(long vmId,
                                                                CPanelAction.ActionType actionType,
                                                                CPanelAction.Status status) {
                CPanelAction cPanelAction = this.createCPanelAction(vmId, actionType, status);
                this.storeCPanelAction(cPanelAction);
                return cPanelAction;
            }

            @Override
            public CPanelAction requestAccess(long vmId, String publicIp, String fromIp) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelAction getAction(long actionId) {
                if (!cPanelActions.containsKey(actionId)) {
                    throw new NotFoundException("CPanel action not present");
                }

                return cPanelActions.get(actionId);
            }

            @Override
            public CPanelAction imagePrep(long l) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelAction imageConfig(long vmId) {
                return this.createAndStoreCPanelAction(
                        vmId, CPanelAction.ActionType.ImageConfig, CPanelAction.Status.COMPLETE);
            }

            @Override
            public CPanelAction licenseRefresh(long vmId) {
                return this.createAndStoreCPanelAction(
                        vmId, CPanelAction.ActionType.LicenseRefresh, CPanelAction.Status.COMPLETE);
            }

            @Override
            public CPanelAction licenseActivate(long l) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelAction licenseRelease(long vmId) {
                return this.createAndStoreCPanelAction(
                        vmId, CPanelAction.ActionType.LicenseRelease, CPanelAction.Status.COMPLETE);
            }

            @Override
            public CPanelLicense getLicenseFromDb(long l) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelAction licenseUpdateIP(long l, String s, String s1) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public void onComplete(CompleteResponse completeResponse) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelAction getcPanelPublicIp(long arg0) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public CPanelAction requestSiteList(long arg0, String arg1) {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    @Provides
    public  NodePingService provideNodePingService() {
        return new NodePingService() {
            @Override
            public NodePingAccount createAccount(String s, String s1, String s2) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public NodePingAccount getAccount(long l) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public void deleteAccount(long l) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public Response getEvents(long l) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            private NodePingCheck createNodePingCheck(long accountId) {
                NodePingCheck nodePingCheck = new NodePingCheck();
                nodePingCheck.accountId = accountId;
                nodePingCheck.checkId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
                return nodePingCheck;
            }

            private void storeNodePingCheck(long accountId, NodePingCheck nodePingCheck) {
                if (!accountNodePingChecksList.containsKey(accountId)) {
                    accountNodePingChecksList.put(accountId, new ArrayList<>());
                }

                List<NodePingCheck> checks = accountNodePingChecksList.get(accountId);
                checks.add(nodePingCheck);

                nodePingChecks.put(nodePingCheck.checkId, nodePingCheck);
            }

            private NodePingCheck createAndStoreNodePingCheck(long accountId) {
                NodePingCheck nodePingCheck = this.createNodePingCheck(accountId);
                this.storeNodePingCheck(accountId, nodePingCheck);
                return nodePingCheck;
            }

            @Override
            public NodePingCheck createCheck(long accountId, CreateCheckRequest createCheckRequest) {
                return this.createAndStoreNodePingCheck(accountId);
            }

            @Override
            public List<NodePingCheck> getChecks(long accountId) {
                if (!accountNodePingChecksList.containsKey(accountId)) {
                    throw new NotFoundException("Node ping check account not present");
                }

                return accountNodePingChecksList.get(accountId);
            }

            @Override
            public NodePingCheck getCheck(long accountId, long checkId) {
                if (!nodePingChecks.containsKey(checkId)) {
                    throw new NotFoundException("Node ping check not present");
                }

                return nodePingChecks.get(checkId);
            }

            @Override
            public void deleteCheck(long accountId, long checkId) {
                if (!nodePingChecks.containsKey(checkId)) {
                    throw new NotFoundException("Node ping check not present");
                }

                nodePingChecks.remove(checkId);
                List<NodePingCheck> checks = accountNodePingChecksList
                        .get(accountId)
                        .stream()
                        .filter(np -> np.checkId != checkId)
                        .collect(Collectors.toList());
                accountNodePingChecksList.put(accountId, checks);
            }

            @Override
            public Response getCheckResults(long l, long l1) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public List<NodePingUptimeRecord> getCheckUptime(long l, long l1, String s, String s1, String s2) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public List<NodePingEvent> getCheckEvents(long l, long l1, int i) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public Response sendNotification(NodePingNotification notification) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");

            }
        };
    }

    @Provides
    public Vps4MessagingService provideMessagingService(Config vps4Config) {
        return new Vps4MessagingService() {
            @Override
            public Message getMessageById(String messageId) {
                if (!gdMessagingServiceMessages.containsKey(messageId)) {
                    throw new NotFoundException(String.format("Messaging id not found: %s", messageId));
                }

                return gdMessagingServiceMessages.get(messageId);
            }

            private Message createMessage(String shopperId, String messageId) {
                Message message = new Message();
                message.messageId = messageId;
                message.shopperId = shopperId;
                message.templateTypeKey = DefaultVps4MessagingService.EmailTemplates.VirtualPrivateHostingProvisioned4.toString();
                message.templateNamespaceKey = DefaultVps4MessagingService.TEMPLATE_NAMESPACE_KEY;
                message.privateLabelId = 1;
                message.createdAt = DateTime.now().toString();

                return message;
            }

            private void storeMessage(Message message) {
                gdMessagingServiceMessages.put(message.messageId, message);
            }

            private String createFakeMessage(String shopperId) {
                String messageId = UUID.randomUUID().toString();
                Message setupMessage = createMessage(shopperId, messageId);
                storeMessage(setupMessage);

                return messageId;
            }

            @Override
            public String sendSetupEmail(String shopperId, String accountName, String ipAddress, String orionId, boolean isFullyManaged) {
                return createFakeMessage(shopperId);
            }

            @Override
            public String sendFullyManagedEmail(String shopperId, String controlPanel)
                    throws MissingShopperIdException, IOException {
                return createFakeMessage(shopperId);
            }

            @Override
            public String sendScheduledPatchingEmail(String shopperId, String serverName, Instant startTime,
                                                     long durationMinutes, boolean isFullyManaged) {
                return createFakeMessage(shopperId);
            }

            @Override
            public String sendUnexpectedButScheduledMaintenanceEmail(String shopperId, String serverName, Instant startTime,
                    long durationMinutes, boolean isFullyManaged) {
                return createFakeMessage(shopperId);
            }

            @Override
            public String sendSystemDownFailoverEmail(String shopperId, String serverName, boolean isFullyManaged) {
                return createFakeMessage(shopperId);
            }

            @Override
            public String sendFailoverCompletedEmail(String shopperId, String serverName, boolean isFullyManaged) {
                return createFakeMessage(shopperId);
            }
        };
    }

    @Provides
    public MailRelayService provideMailRelayService() {
        return new MailRelayService() {
            private MailRelay createMailRelay(String ipAddress, int quota) {
                MailRelay mailRelay = new MailRelay();
                mailRelay.ipv4Address = ipAddress;
                mailRelay.quota = quota;
                return mailRelay;
            }

            private void storeMailRelay(String ipAddress, MailRelay mailRelay) {
                mailRelays.put(ipAddress, mailRelay);
            }

            private MailRelay createAndStoreMailRelay(String ipAddress, int quota) {
                MailRelay mailRelay = this.createMailRelay(ipAddress, quota);
                this.storeMailRelay(ipAddress, mailRelay);
                return mailRelay;
            }

            @Override
            public MailRelay getMailRelay(String ipAddress) {
                if (!mailRelays.containsKey(ipAddress)) {
                    throw new NotFoundException("Mail relay not set/configured");
                }

                return mailRelays.get(ipAddress);
            }

            @Override
            public MailRelay setRelayQuota(String ipAddress, MailRelayUpdate mailRelayUpdate) {
                return createAndStoreMailRelay(ipAddress, mailRelayUpdate.quota);
            }

            @Override
            public List<MailRelayHistory> getRelayHistory(String ipAddress) {
                return new ArrayList<>();
            }
        };
    }

    @Provides
    public SysAdminService provideMockSysAdminService() {
        return new SysAdminService() {
            private SysAdminAction createSysAdminAction(long vmId,
                                                        SysAdminAction.Type actionType,
                                                        SysAdminAction.Status status) {
                SysAdminAction sysAdminAction = new SysAdminAction();
                sysAdminAction.sysAdminActionId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
                sysAdminAction.vmId = vmId;
                sysAdminAction.type = actionType;
                sysAdminAction.status = status;
                return sysAdminAction;
            }

            private void storeSysAdminAction(long vmId, SysAdminAction sysAdminAction) {
                if (!sysAdminActionList.containsKey(vmId)) {
                    sysAdminActionList.put(vmId, new ArrayList<>());
                }

                List<SysAdminAction> actions = sysAdminActionList.get(vmId);
                actions.add(sysAdminAction);

                sysAdminActions.put(sysAdminAction.sysAdminActionId, sysAdminAction);
            }

            private SysAdminAction createAndStoreSysAdminAction(long vmId,
                                                                SysAdminAction.Type actionType,
                                                                SysAdminAction.Status status) {
                SysAdminAction sysAdminAction = this.createSysAdminAction(vmId, actionType, status);
                this.storeSysAdminAction(vmId, sysAdminAction);
                return sysAdminAction;
            }

            @Override
            public SysAdminAction addUser(long vmId, String username, String password) {
                return this.createAndStoreSysAdminAction(
                        vmId, SysAdminAction.Type.ADD_USER, SysAdminAction.Status.COMPLETE);
            }

            @Override
            public SysAdminAction removeUser(long vmId, String username) {
                return this.createAndStoreSysAdminAction(
                        vmId, SysAdminAction.Type.REMOVE_USER, SysAdminAction.Status.COMPLETE);
            }

            @Override
            public SysAdminAction changePassword(long vmId, String username, String password, String controlPanel) {
                return this.createAndStoreSysAdminAction(
                        vmId, SysAdminAction.Type.CHANGE_PASSWORD, SysAdminAction.Status.COMPLETE);
            }

            @Override
            public SysAdminAction changeHostname(long vmId, String hostname, String controlPanel) {
                return this.createAndStoreSysAdminAction(
                        vmId, SysAdminAction.Type.CHANGE_HOSTNAME, SysAdminAction.Status.COMPLETE);
            }

            @Override
            public SysAdminAction addFTPUser(long l, String s, String s1, String s2, String s3) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction removeFTPUser(long l, String s, String s1, String s2) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction changeFTPPassword(long l, String s, String s1, String s2, String s3) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction addDBUser(long l, String s, String s1, String s2, String s3, String s4) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction removeDBUser(long l, String s, String s1, String s2, String s3) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction changeDBPassword(long l, String s, String s1, String s2, String s3, String s4) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction addSite(long l, String s, String s1, String s2) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction removeSite(long l, String s, String s1) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction addDB(long l, String s, String s1, String s2, String s3) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction removeDB(long l, String s, String s1, String s2, String s3) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction getSysAdminAction(long sysAdminActionId) {
                if (!sysAdminActions.containsKey(sysAdminActionId)) {
                    throw new NotFoundException("Sysadmin action not present");
                }

                return sysAdminActions.get(sysAdminActionId);
            }

            @Override
            public List<SysAdminAction> getSysAdminActions(int i, int i1) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public List<SysAdminAction> getSysAdminActionsByServer(long l, int i, int i1) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public List<SysAdminInstallable> getInstallables(long l) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction installPackage(long l, long l1) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public List<SysAdminInstallable> getInstallations(long l) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction uninstallPackage(long l, long l1) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction usageStatsUpdate(long l, int i) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public Response usageStatsResults(long l, String s, String s1) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction configureMTA(long vmId, String controlPanel) {
                return this.createAndStoreSysAdminAction(
                        vmId, SysAdminAction.Type.CONFIGURE_MTA, SysAdminAction.Status.COMPLETE);
            }

            @Override
            public SysAdminAction enableAdmin(long vmId, String username) {
                return this.createAndStoreSysAdminAction(
                        vmId, SysAdminAction.Type.ENABLE_ADMIN, SysAdminAction.Status.COMPLETE);
            }

            @Override
            public SysAdminAction disableAdmin(long vmId, String username) {
                return this.createAndStoreSysAdminAction(
                        vmId, SysAdminAction.Type.DISABLE_ADMIN, SysAdminAction.Status.COMPLETE);
            }

            @Override
            public void onComplete(CompleteResponse completeResponse) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public SysAdminAction installNydus(long l) {
                return null;
            }

            @Override
            public SysAdminAction removeNydus(long l) {
                return null;
            }
        };
    }

    private class VmActionEntry {
        public long accessCount;
        public VmAction action;

        public VmActionEntry(VmAction action) {
            this.action = action;
            this.accessCount = 0;
        }
    }

    @Provides
    public VmService provideMockVmService() {
        return new VmService() {
            private String generateRandomIpAddress() {
                Random r = new Random();
                return String.format(
                        "%d.%d.%d.%d", r.nextInt(256), r.nextInt(256),
                        r.nextInt(256), r.nextInt(256));
            }

            private Vm createVmHelper(String sgid) {
                Vm vm = new Vm();
                vm.vmId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
                vm.sgid = sgid;
                vm.status = "ACTIVE";
                vm.address = new VmAddress();
                vm.address.ip_address = this.generateRandomIpAddress();
                vm.address.netmask = "255.255.0.0";
                vm.address.gateway = "1.1.1.1";
                return vm;
            }

            private VmAction createVmAction(
                    long vmId, String actionType, VmAction.Status status) {
                VmAction vmAction = new VmAction();
                vmAction.vmActionId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
                vmAction.vmId = vmId;
                vmAction.actionType = actionType;
                vmAction.state = status;
                vmAction.vmId = vmId;
                return vmAction;
            }

            private void storeVmAction(long vmId, VmAction vmAction) {
                if (!vmActionList.containsKey(vmId)) {
                    vmActionList.put(vmId, new ArrayList<>());
                }

                List<VmAction> actions = vmActionList.get(vmId);
                actions.add(vmAction);

                vmActions.put(vmAction.vmActionId, new VmActionEntry(vmAction));
            }

            private VmAction createAndStoreVmAction(
                    long vmId, String actionType, VmAction.Status status) {
                VmAction vmAction = this.createVmAction(vmId, actionType, status);
                this.storeVmAction(vmId, vmAction);
                return vmAction;
            }

            private Boolean isVmDestroyed(long vmId) {
                return vmActionList.get(vmId).stream().anyMatch(va -> va.actionType.equals("DESTROYED"));
            }

            @Override
            public Console getConsole(long vmId) {
                Console console = new Console();
                console.url = "https://console.phx-public.cloud.secureserver.net:443/spice_auto.html?token=394f9629-4081-421d-a2e3-30b7aa950843";
                return console;
            }

            @Override
            public VmAction createConsoleUrl(long vmId) {
                VmAction vmAction = this.createVmAction(vmId, "CREATE_CONSOLE", VmAction.Status.COMPLETE);
                this.storeVmAction(vmId, vmAction);
                return vmAction;
            }

            @Override
            public Vm getVm(long vmId) {
                if (!customerVms.containsKey(vmId)) {
                    throw new NotFoundException("Vm not present");
                }

                return customerVms.get(vmId);
            }

            @Override
            public VmAction getVmAction(long vmId, long vmActionId) {
                if (!vmActions.containsKey(vmActionId)) {
                    throw new NotFoundException("Vm action not present");
                }

                VmActionEntry entry = vmActions.get(vmActionId);
                entry.accessCount++;
                // Mark the action as complete after 3 tries
                entry.action.state =  entry.accessCount > 3
                        ? VmAction.Status.COMPLETE
                        : VmAction.Status.IN_PROGRESS;

                return entry.action;
            }

            @Override
            public VmList listVms(String s) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public VmAction createVm(CreateVMRequest createVMRequest) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public VmAction createVmWithFlavor(CreateVMWithFlavorRequest createVMWithFlavorRequest) {
                Vm vm = this.createVmHelper(createVMWithFlavorRequest.sgid);
                customerVms.put(vm.vmId, vm);

                return this.createAndStoreVmAction(vm.vmId, "CREATE", VmAction.Status.NEW);
            }

            @Override
            public VmAction destroyVm(long vmId) {
                if (!customerVms.containsKey(vmId)
                        || !vmActionList.containsKey(vmId)
                        || this.isVmDestroyed(vmId)) {
                    throw new NotFoundException("Vm not present or has already been destroyed");
                }

                customerVms.get(vmId).status = "DESTROYED";
                return this.createAndStoreVmAction(vmId, "DESTROY", VmAction.Status.COMPLETE);
            }

            @Override
            public FlavorList listFlavors() {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public VmList getVmsBulk(String s) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public VmAction startVm(long vmId) {
                if (!customerVms.containsKey(vmId)
                        || !vmActionList.containsKey(vmId)
                        || this.isVmDestroyed(vmId)) {
                    throw new NotFoundException("Vm not present or has already been destroyed");
                }

                return this.createAndStoreVmAction(vmId, "START", VmAction.Status.NEW);
            }

            @Override
            public VmAction stopVm(long vmId) {
                if (!customerVms.containsKey(vmId)
                        || !vmActionList.containsKey(vmId)
                        || this.isVmDestroyed(vmId)) {
                    throw new NotFoundException("Vm not present or has already been destroyed");
                }

                return this.createAndStoreVmAction(vmId, "STOP", VmAction.Status.NEW);
            }

            @Override
            public VmAction rebootVm(long vmId) {
                if (!customerVms.containsKey(vmId)
                        || !vmActionList.containsKey(vmId)
                        || this.isVmDestroyed(vmId)) {
                    throw new NotFoundException("Vm not present or has already been destroyed");
                }

                return this.createAndStoreVmAction(vmId, "RESTART", VmAction.Status.NEW);
            }

            @Override
            public VmAction rebuildVm(long vmId) {
                if (!customerVms.containsKey(vmId)
                        || !vmActionList.containsKey(vmId)
                        || this.isVmDestroyed(vmId)) {
                    throw new NotFoundException("Vm not present or has already been destroyed");
                }

                return this.createAndStoreVmAction(vmId, "REBUILD", VmAction.Status.NEW);
            }
        };
    }

    private class AddressActionEntry {
        public long accessCount;
        public AddressAction action;

        public AddressActionEntry(AddressAction action) {
            this.action = action;
            this.accessCount = 0;
        }
    }

    @Provides
    public NetworkServiceV2 provideMockNetworkService() {
        return new NetworkServiceV2() {
            private String generateRandomIpAddress() {
                Random r = new Random();
                return String.format(
                        "%d.%d.%d.%d", r.nextInt(256), r.nextInt(256),
                        r.nextInt(256), r.nextInt(256));
            }

            private IpAddress createAddressHelper(String sgid, String zone) {
                IpAddress ipAddress = new IpAddress();
                ipAddress.addressId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
                ipAddress.address = this.generateRandomIpAddress();
                ipAddress.status = IpAddress.Status.UNBOUND;
                return ipAddress;
            }

            private AddressAction createAddressAction(
                    long addressId, AddressAction.Type actionType, AddressAction.Status status) {
                AddressAction addressAction = new AddressAction();
                addressAction.addressActionId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
                addressAction.addressId = addressId;
                addressAction.type = actionType;
                addressAction.status = status;
                return addressAction;
            }

            private void storeAddressAction(long addressId, AddressAction addressAction) {
                if (!addressActionList.containsKey(addressId)) {
                    addressActionList.put(addressId, new ArrayList<>());
                }

                List<AddressAction> actions = addressActionList.get(addressId);
                actions.add(addressAction);

                addressActions.put(addressAction.addressActionId, new AddressActionEntry(addressAction));
            }

            private AddressAction createAndStoreAddressAction(
                    long addressId, AddressAction.Type actionType, AddressAction.Status status) {
                AddressAction addressAction = this.createAddressAction(addressId, actionType, status);
                this.storeAddressAction(addressId, addressAction);
                return addressAction;
            }

            private Boolean isAddressReleased(Long addressId) {
                return addressActionList.get(addressId).stream().anyMatch(sa -> sa.type == AddressAction.Type.RELEASE);
            }

            private void updateIpAddress(long addressId, long serverId, IpAddress.Status status) {
                IpAddress address = customerAddresses.get(addressId);
                address.status = status;
                address.serverId = serverId;
            }

            @Override
            public AddressAction acquireIp(String sgid, String zone) {
                IpAddress address = this.createAddressHelper(sgid, zone);
                customerAddresses.put(address.addressId, address);

                return this.createAndStoreAddressAction(
                        address.addressId, AddressAction.Type.ACQUIRE, AddressAction.Status.NEW);
            }

            @Override
            public AddressAction bindIp(long addressId, long vmId, Boolean force) {
                if (!customerAddresses.containsKey(addressId)
                        || !addressActionList.containsKey(addressId)
                        || this.isAddressReleased(addressId)) {
                    throw new NotFoundException("Address not present or has already been released");
                }

                this.updateIpAddress(addressId, vmId, IpAddress.Status.BOUND);
                return this.createAndStoreAddressAction(
                        addressId, AddressAction.Type.BIND, AddressAction.Status.NEW);
            }

            @Override
            public AddressAction unbindIp(long addressId, Boolean force) {
                if (!customerAddresses.containsKey(addressId)
                        || !addressActionList.containsKey(addressId)
                        || this.isAddressReleased(addressId)) {
                    throw new NotFoundException("Address not present or has already been released");
                }

                this.updateIpAddress(addressId, 0, IpAddress.Status.BOUND);
                return this.createAndStoreAddressAction(
                        addressId, AddressAction.Type.UNBIND, AddressAction.Status.NEW);
            }

            @Override
            public AddressAction releaseIp(long addressId) {
                if (!customerAddresses.containsKey(addressId)
                        || !addressActionList.containsKey(addressId)
                        || this.isAddressReleased(addressId)) {
                    throw new NotFoundException("Address not present or has already been released");
                }

                this.updateIpAddress(addressId, 0, IpAddress.Status.RELEASED);
                return this.createAndStoreAddressAction(
                        addressId, AddressAction.Type.RELEASE, AddressAction.Status.NEW);
            }

            @Override
            public IpAddressList listIps(String sgid, int offset, int limit, IpAddress.Status status, String ipAddress) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public IpAddress getAddress(long addressId) {
                if (!customerAddresses.containsKey(addressId)) {
                    throw new NotFoundException("Address not present");
                }

                return customerAddresses.get(addressId);
            }

            @Override
            public IpAddressList getServerAddressIds(long addressId, int offset, int limit, IpAddress.Status status) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public IpAddressList getAddressesForStatus(IpAddress.Status status, int offset, int limit) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public AddressAction getAddressAction(long addressId, long addressActionId) {
                if (!addressActions.containsKey(addressActionId)) {
                    throw new NotFoundException("Address action not present");
                }

                AddressActionEntry entry = addressActions.get(addressActionId);
                entry.accessCount++;
                // Mark the action as complete after 3 tries
                entry.action.status =  entry.accessCount > 3
                        ? AddressAction.Status.COMPLETE
                        : AddressAction.Status.IN_PROGRESS;

                return entry.action;
            }

            @Override
            public AddressActionList getActions(long addressId, int offset, int limit) {
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
                if (!eCommAccounts.containsKey(account.account_guid)) {
                    account.product_meta = new HashMap<>();
                    eCommAccounts.put(account.account_guid, account);

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
                return eCommAccounts.get(accountGuid);
            }

            @Override
            public Account updateAccount(String accountGuid, Account account) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public Map<String, String> updateProductMetadata(String accountGuid, MetadataUpdate metadataUpdate) {
                if (eCommAccounts.containsKey(accountGuid)) {
                    Account account = eCommAccounts.get(accountGuid);
                    if (account.product_meta.equals(metadataUpdate.from)) {
                        account.product_meta = metadataUpdate.to;
                        return account.product_meta;
                    }
                }

                return null;
            }

            @Override
            public void deleteAccount(String accountGuid) {
                if (eCommAccounts.containsKey(accountGuid)) {
                    Account account = eCommAccounts.get(accountGuid);
                    account.status = Account.Status.removed;
                }
            }

            @Override
            public Response setCommonName(String arg0, ECommDataCache arg1) {
                return null;
            }

            @Override
            public Account updateAccountStatusAndPlanFeatures(String arg0,
                    Account arg1) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Not implemented, yet");
            }
        };
    }
}
