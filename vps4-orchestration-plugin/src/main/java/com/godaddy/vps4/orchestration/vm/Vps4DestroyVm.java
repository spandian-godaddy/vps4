package com.godaddy.vps4.orchestration.vm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.orchestration.cdn.Vps4RemoveCdnSite;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.monitoring.RemovePanoptaMonitoring;
import com.godaddy.vps4.orchestration.scheduler.DeleteAutomaticBackupSchedule;
import com.godaddy.vps4.orchestration.scheduler.ScheduleDestroyVm;
import com.godaddy.vps4.orchestration.snapshot.Vps4DestroySnapshot;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4DestroyVm",
        requestType=Vps4DestroyVm.Request.class,
        responseType=Vps4DestroyVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DestroyVm extends ActionCommand<Vps4DestroyVm.Request, Vps4DestroyVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyVm.class);
    private final NetworkService networkService;
    private final CdnDataService cdnDataService;
    private final ShopperNotesService shopperNotesService;
    private final SnapshotService snapshotService;
    private final VirtualMachineService virtualMachineService;
    private CommandContext context;
    private VirtualMachine vm;
    private String gdUserName;
    private String shopperId;
    private byte[] encryptedCustomerJwt;
    private long actionId;

    @Inject
    public Vps4DestroyVm(ActionService actionService,
                         NetworkService networkService,
                         ShopperNotesService shopperNotesService,
                         SnapshotService snapshotService,
                         VirtualMachineService virtualMachineService,
                         CdnDataService cdnDataService) {
        super(actionService);
        this.networkService = networkService;
        this.shopperNotesService = shopperNotesService;
        this.snapshotService = snapshotService;
        this.virtualMachineService = virtualMachineService;
        this.cdnDataService = cdnDataService;
    }

    @Override
    public Response executeWithAction(CommandContext context, Request request) {
        this.context = context;
        this.vm = request.virtualMachine;
        this.gdUserName = request.gdUserName;
        this.shopperId = request.shopperId;
        this.encryptedCustomerJwt = request.encryptedCustomerJwt;
        this.actionId = request.actionId;

        logger.info("Destroying server {}", vm.vmId);

        try {
            unclaimCredit();
            deleteVm();
            cancelIncompleteVmActions();
            destroyVmSnapshots(vm.vmId);
            unlicenseControlPanel();
            removeMonitoring();
            removeIp();
            getAndRemoveActiveCdnSites();
            deleteAutomaticBackupSchedule();
            deleteAllScheduledJobsForVm();
            deleteSupportUsersInDatabase();
            VmAction hfsAction = deleteVmInHfs(request);
            writeShopperNote();
            return buildResponse(hfsAction);
        } catch (Exception e) {
            rescheduleDestroy();
            throw e;
        }
    }

    private Response buildResponse(VmAction hfsAction) {
        logger.info("Completed destroying VM {}", vm.vmId);

        Vps4DestroyVm.Response response = new Vps4DestroyVm.Response();
        response.vmId = vm.vmId;
        response.hfsAction = hfsAction;
        return response;
    }

    private void rescheduleDestroy() {
        context.execute(ScheduleDestroyVm.class, vm.vmId);
    }

    private VmAction deleteVmInHfs(Request request) {
        DestroyVm.Request destroyVmRequest = new DestroyVm.Request();
        destroyVmRequest.hfsVmId = vm.hfsVmId;
        destroyVmRequest.actionId = request.actionId;
        return context.execute("DestroyVmHfs", DestroyVm.class, destroyVmRequest);
    }

    private void deleteSupportUsersInDatabase() {
        context.execute(Vps4RemoveSupportUsersFromDatabase.class, vm.vmId);
    }

    private void deleteAllScheduledJobsForVm() {
        context.execute(Vps4DeleteAllScheduledJobsForVm.class, vm.vmId);
    }

    private void removeMonitoring() {
        context.execute(RemovePanoptaMonitoring.class, vm.vmId);
    }

    private void removeIp() {
        IpAddress address = networkService.getVmPrimaryAddress(vm.vmId);
        if (isAddressValid(address)) {
            removeIpFromServer(address);
            updateIpValidUntil(address);
        }
        //clean up additional ips if they exist in OH and Ded
        getAndRemoveAdditionalIps();
    }
    private void getAndRemoveAdditionalIps() {
        List<IpAddress> additionalIps;
        additionalIps = networkService.getVmActiveSecondaryAddresses(vm.hfsVmId);
        if (additionalIps != null) {
            for (IpAddress ip : additionalIps) {
                context.execute("RemoveIp-" + ip.addressId, Vps4RemoveIp.class, ip);
                context.execute("MarkIpDeleted-" + ip.addressId, ctx -> {
                    networkService.destroyIpAddress(ip.addressId);
                    return null;
                }, Void.class);
            }
        }
    }

    private boolean isAddressValid(IpAddress address) {
        return address != null && address.validUntil.isAfter(Instant.now());
    }


    private void updateIpValidUntil(IpAddress address) {
        context.execute("MarkIpDeleted-" + address.addressId, ctx -> {
                networkService.destroyIpAddress(address.addressId);
                return null;
            }, Void.class);
    }

    protected void removeIpFromServer(IpAddress address) {
        context.execute("RemoveIp-" + address.addressId, Vps4RemoveIp.class, address);
    }

    private void getAndRemoveActiveCdnSites() {
        List<VmCdnSite> activeCdnSites = cdnDataService.getActiveCdnSitesOfVm(vm.vmId);
        if (activeCdnSites != null) {
            for (VmCdnSite site : activeCdnSites) {
                Vps4RemoveCdnSite.Request req = new Vps4RemoveCdnSite.Request();
                req.vmId = vm.vmId;
                req.siteId = site.siteId;
                req.shopperId = this.shopperId;
                req.encryptedCustomerJwt = this.encryptedCustomerJwt;
                context.execute("RemoveCdnSite-" + site.siteId, Vps4RemoveCdnSite.class, req);
            }
        }
    }

    private void deleteAutomaticBackupSchedule() {
        if (hasAutomaticBackupJobScheduled(vm)) {
            try {
                context.execute(DeleteAutomaticBackupSchedule.class, vm.backupJobId);
            } catch (RuntimeException e) {
                // squelch this for now. dont fail a vm deletion just because we couldn't delete an auto backup schedule
                // TODO: should this behaviour be changed?
                logger.error("Automatic backup job schedule deletion failed");
            }
        }
    }

    private boolean hasAutomaticBackupJobScheduled(VirtualMachine vm) {
        return vm.backupJobId != null;
    }

    private void unlicenseControlPanel() {
        context.execute(UnlicenseControlPanel.class, vm);
    }

    private void writeShopperNote() {
        try {
            String shopperNote = String.format("Server was destroyed by %s. VM ID: %s. Credit ID: %s.",
                                               gdUserName, vm.vmId, vm.orionGuid);
            shopperNotesService.processShopperMessage(vm.vmId, shopperNote);
        } catch (Exception ignored) {}
    }

    private void destroyVmSnapshots(UUID vmId) {
        snapshotService
                .getSnapshotsForVm(vmId)
                .stream()
                .filter(s -> s.status != SnapshotStatus.DESTROYED && s.status != SnapshotStatus.CANCELLED)
                .forEach(snapshot -> {
                    if (snapshot.status == SnapshotStatus.NEW
                            || snapshot.status == SnapshotStatus.ERROR
                            || snapshot.status == SnapshotStatus.ERROR_RESCHEDULED
                            || snapshot.status == SnapshotStatus.LIMIT_RESCHEDULED
                            || snapshot.status == SnapshotStatus.AGENT_DOWN) {
                        // just mark snapshots as cancelled if they were new or errored
                        snapshotService.updateSnapshotStatus(snapshot.id, SnapshotStatus.CANCELLED);
                    } else {
                        Vps4DestroySnapshot.Request request = new Vps4DestroySnapshot.Request();
                        request.hfsSnapshotId = snapshot.hfsSnapshotId;
                        request.vps4SnapshotId = snapshot.id;
                        context.execute("Vps4DestroySnapshot-" + snapshot.id, Vps4DestroySnapshot.class, request);
                    }
                });
    }

    private void cancelIncompleteVmActions() {
        List<Action> actions = actionService.getIncompleteActions(vm.vmId);
        if (actions.stream().anyMatch(a -> a.type.equals(ActionType.CREATE_VM))) {
            throw new RuntimeException("Create action is already running");
        }
        for (Action action : actions) {
            if (action.id != actionId) {
                context.execute("MarkActionCancelled-" + action.id, ctx -> {
                    String note = "Action cancelled via api by admin";
                    actionService.cancelAction(action.id, new JSONObject().toJSONString(), note);
                    return null;
                }, Void.class);
            }
        }
    }

    private void deleteVm() {
        context.execute("MarkVmDeleted", ctx -> {
            virtualMachineService.setVmRemoved(vm.vmId);
            return null;
        }, Void.class);
    }

    private void unclaimCredit() {
        context.execute(Vps4UnclaimCredit.class, vm);
    }

    public static class Request extends VmActionRequest {
        public String gdUserName;
        public String shopperId;
        public byte[] encryptedCustomerJwt;
    }

    public static class Response {
        public UUID vmId;
        public VmAction hfsAction;
    }

}
