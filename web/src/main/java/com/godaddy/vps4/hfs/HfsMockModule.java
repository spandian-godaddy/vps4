package com.godaddy.vps4.hfs;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.mockito.Mockito;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayHistory;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.mailrelay.MailRelayUpdate;
import com.godaddy.hfs.vm.Console;
import com.godaddy.hfs.vm.CreateVMRequest;
import com.godaddy.hfs.vm.CreateVMWithFlavorRequest;
import com.godaddy.hfs.vm.FlavorList;
import com.godaddy.hfs.vm.ServerUsageStats;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmAddress;
import com.godaddy.hfs.vm.VmList;
import com.godaddy.hfs.vm.VmOSInfo;
import com.godaddy.hfs.vm.VmService;
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
import gdg.hfs.vhfs.nodeping.NodePingEvent;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.nodeping.NodePingUptimeRecord;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class HfsMockModule extends AbstractModule {

    private static final long actionId = 123;
    private static final CPanelAction reqAccessRet;
    private static final CPanelAction getActionRet;
    private static final VmAction createVmAction;
    private static final MailRelay mailRelay;
    private static final List<MailRelayHistory> mailRelayHistory;
    private static final Map<String, Account> ecommAccounts;
    private static final Map<String, List<Account>> shopperECommAccounts;

    static final String accessHash = "b32b85d55e3d94408b78f729455e0076"
            + "930065e534418a463322bd966edc5a1e"
            + "bc7b61ddfbeb2958a6f276c56fd90ba3"
            + "0ce8d33ecbcab48d4500a32d465c98ac"
            + "5d8d9ce03c82702b0fb64d5813753284"
            + "b66087dac6937ff62f2074b71c14b173"
            + "e9dc90397b2c548dbe0cf16eecc114da"
            + "e25a6d54e28c64195f0141881b012594"
            + "107764759dbbb30af52b26bbeb1f4a58"
            + "15401bdef541893243cb04dee1fc4f0a"
            + "ebae588f6ce6dbe6ed160e644a5da3c4"
            + "edff957b4e79cc0914ab952e7dd665a8"
            + "f09abe02e9d32058713661f35a974716"
            + "830b108f2372305ceba0e8342e1b11fc"
            + "61f10dbf9b195697647c7d4e8afafe72"
            + "45a74dffb07250fc213f80cdd6dde280"
            + "b9b81a2c6637215b98a51601a32c5d8d"
            + "6382f11e086a3f0f9bd5a999051729be"
            + "f661e8a731ec34603261d260101d9451"
            + "83c459cb9e1681e040e414006ca5bc99"
            + "b7354ff60959bcea14ac702dd4db2099"
            + "002bfc5f7550811bf2a639c1115d75ad"
            + "153108ac3b2fde2d266334037975f8d4"
            + "e083f7a2929d40bb7b9abcd1b5405340"
            + "fe93a9521bf49bef92937bc3121137f2"
            + "0a022fc590d01c1624e8200645ec0856"
            + "41e1f0a3dc939f8b42cc2d94ba02b4f0"
            + "593c8fe788d2eeca1be2580935a35da8"
            + "a209ef0bf98300d4f2f1c610255b8852"
            + "ecf8786a830798f564102fdd6fd0faec";

    static {
        reqAccessRet = new CPanelAction();
        reqAccessRet.status = CPanelAction.Status.IN_PROGRESS;
        reqAccessRet.actionId = actionId;

        getActionRet = new CPanelAction();
        getActionRet.status = CPanelAction.Status.COMPLETE;
        getActionRet.actionId = actionId;
        getActionRet.responsePayload = String.format("{\"cphash\":\"%s\",\"success\":true,\"error\":null}", accessHash);

        createVmAction = new VmAction();
        createVmAction.vmActionId = new Random().nextInt(100000);
        createVmAction.vmId = 0;
        createVmAction.state = VmAction.Status.COMPLETE;
        createVmAction.tickNum = 1;

        mailRelay = new MailRelay();
        mailRelay.quota = 5000;
        mailRelay.relays = 1000;

        MailRelayHistory history = new MailRelayHistory();
        history.date = DateTime.now().toString();
        history.quota = 5000;
        history.relays = 1234;
        mailRelayHistory = new ArrayList<>();
        mailRelayHistory.add(history);
        ecommAccounts = new HashMap<>();
        shopperECommAccounts = new HashMap<>();
    }

    @Override
    public void configure() {
        SysAdminService sysAdminService = buildSysAdminService();
        bind(SysAdminService.class).toInstance(sysAdminService);
        PleskService pleskService = Mockito.mock(PleskService.class);
        bind(PleskService.class).toInstance(pleskService);
        NodePingService nodePingService = buildNodePingService();
        bind(NodePingService.class).toInstance(nodePingService);
    }

    private NodePingService buildNodePingService() {
        NodePingService nodePingService = Mockito.mock(NodePingService.class);
        List<NodePingEvent> events = new ArrayList<>();
        Mockito.when(nodePingService.getCheckEvents(anyLong(), anyLong(), anyInt())).thenReturn(events);
        List<NodePingUptimeRecord> records = new ArrayList<>();
        Mockito.when(nodePingService.getCheckUptime(anyLong(), anyLong(), anyString(), anyString(), anyString())).thenReturn(records);

        return nodePingService;
    }

    private SysAdminService buildSysAdminService() {
        SysAdminService sysAdminService = Mockito.mock(SysAdminService.class);
        SysAdminAction completeAction = new SysAdminAction();
        completeAction.status = SysAdminAction.Status.COMPLETE;

        Mockito.when(sysAdminService.getSysAdminAction(Mockito.anyLong())).thenReturn(completeAction);

        return sysAdminService;

    }

    @Provides
    public VmService buildMockVmService() {
        return new VmService() {

            @Override
            public VmAction createVm(CreateVMRequest arg0) {
                return createVmAction;
            }

            @Override
            public VmAction createVmWithFlavor(CreateVMWithFlavorRequest arg0) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }


            @Override
            public Console getConsole(long vmId) {
                Console console = new Console();
                console.url = "https://console.phx-public.cloud.secureserver.net:443/spice_auto.html?token=394f9629-4081-421d-a2e3-30b7aa950843";
                return console;
            }

            @Override
            public VmAction createConsoleUrl(long vmId) {
                VmAction consoleUrlAction = new VmAction();
                consoleUrlAction.vmActionId = actionId;
                consoleUrlAction.vmId = vmId;
                consoleUrlAction.state = VmAction.Status.COMPLETE;
                consoleUrlAction.tickNum = 1;
                return consoleUrlAction;
            }

            @Override
            public VmAction destroyVm(long vmId) {
                VmAction delAction = new VmAction();
                delAction.vmActionId = 1111;
                delAction.vmId = vmId;
                delAction.state = VmAction.Status.COMPLETE;
                delAction.tickNum = 1;
                return delAction;
            }

            @Override
            public Vm getVm(long vmId) {
                Vm vm = new Vm();
                vm.vmId = vmId;
                vm.status = "ACTIVE";
                vm.address = new VmAddress();
                vm.address.ip_address = "132.148.82.152";
                vm.osinfo = new VmOSInfo();
                vm.osinfo.name = "CentOS-7";
                return vm;
            }

            @Override
            public VmAction getVmAction(long vmId, long actionId) {
                VmAction startVmAction = new VmAction();
                startVmAction.vmActionId = actionId;
                startVmAction.vmId = vmId;
                startVmAction.state = VmAction.Status.COMPLETE;
                startVmAction.tickNum = 1;
                return startVmAction;
            }

            @Override
            public VmList getVmsBulk(String arg0) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public FlavorList listFlavors() {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public VmList listVms(String arg0) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public VmAction startVm(long vmId) {
                // NOTE: do nothing, Implement when needed
                VmAction startVmAction = new VmAction();
                startVmAction.vmActionId = new Random().nextInt(10000);
                startVmAction.vmId = vmId;
                startVmAction.state = VmAction.Status.COMPLETE;
                startVmAction.tickNum = 1;
                return startVmAction;
            }

            @Override
            public VmAction stopVm(long vmId) {
                VmAction stopVmAction = new VmAction();
                stopVmAction.vmActionId = new Random().nextInt(10000);
                stopVmAction.vmId = vmId;
                stopVmAction.state = VmAction.Status.COMPLETE;
                stopVmAction.tickNum = 1;
                return stopVmAction;
            }

            @Override
            public VmAction rebootVm(long vmId) {
                VmAction restartVmAction = new VmAction();
                restartVmAction.vmActionId = new Random().nextInt(10000);
                restartVmAction.vmId = vmId;
                restartVmAction.state = VmAction.Status.COMPLETE;
                restartVmAction.tickNum = 1;
                return restartVmAction;
            }

            @Override
            public VmAction rebuildVm(long vmId) {
                VmAction rebuildVmAction = new VmAction();
                rebuildVmAction.vmActionId = new Random().nextInt(10000);
                rebuildVmAction.vmId = vmId;
                rebuildVmAction.state = VmAction.Status.COMPLETE;
                rebuildVmAction.tickNum = 1;
                return rebuildVmAction;
            }

            @Override
            public ServerUsageStats updateServerUsageStats(long vmId) {
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public ServerUsageStats getServerUsageStats(long vmId, long utilizationId) {
                throw new UnsupportedOperationException("Not implemented, yet");
            }
        };
    }

    @Provides
    public CPanelService provideCPanelService() {

        return new CPanelService() {

            @Override
            public CPanelAction getAction(long arg0) {
                return getActionRet;
            }

            @Override
            public CPanelAction requestAccess(long arg0, String arg1, String arg2) {
                return reqAccessRet;
            }

            @Override
            public CPanelAction imagePrep(long arg0) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelAction licenseActivate(long arg0) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelAction licenseRefresh(long arg0) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelAction licenseRelease(long arg0) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelLicense getLicenseFromDb(long arg0) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelAction licenseUpdateIP(long arg0, String arg1, String arg2) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public void onComplete(CompleteResponse arg0) {
                // NOTE: do nothing, Implement when needed
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public CPanelAction imageConfig(long arg0) {
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
    public MailRelayService provideMailRelayService() {

        return new MailRelayService() {

            @Override
            public MailRelay getMailRelay(String arg0) {
                return mailRelay;
            }

            @Override
            public List<MailRelayHistory> getRelayHistory(String arg0) {
                return mailRelayHistory;
            }

            @Override
            public MailRelay setRelayQuota(String arg0, MailRelayUpdate arg1) {
                MailRelay relay = new MailRelay();
                relay.quota = arg1.quota;
                relay.relays = 0;
                return relay;
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

            @Override
            public void deleteAccount(String arg0) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Not implemented, yet");
            }

            @Override
            public Response setCommonName(String arg0, ECommDataCache arg1) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Not implemented, yet");
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
