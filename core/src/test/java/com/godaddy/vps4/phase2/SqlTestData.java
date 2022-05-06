package com.godaddy.vps4.phase2;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.customNotes.CustomNotesService;
import com.godaddy.vps4.customNotes.jdbc.JdbcCustomNotesService;
import com.godaddy.vps4.notifications.NotificationExtendedDetails;
import com.godaddy.vps4.notifications.NotificationFilter;
import com.godaddy.vps4.notifications.NotificationService;
import com.godaddy.vps4.notifications.NotificationType;
import com.godaddy.vps4.notifications.jdbc.JdbcNotificationService;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduledJob.jdbc.JdbcScheduledJobService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;


public class SqlTestData {

    public static long getNextHfsVmId(DataSource dataSource) {
        return Sql.with(dataSource).exec("SELECT max(hfs_vm_id) as hfs_vm_id FROM virtual_machine",
                Sql.nextOrNull(rs -> rs.isAfterLast() ? 0 : rs.getLong("hfs_vm_id"))) + 1;
    }

    public static long getNextHfsAddressId(DataSource dataSource) {
        return Sql.with(dataSource).exec("SELECT max(hfs_address_id) as hfs_address_id FROM ip_address",
                Sql.nextOrNull(rs -> rs.isAfterLast() ? 0 : rs.getLong("hfs_address_id"))) + 1;
    }

    public static VirtualMachine insertTestVm(UUID orionGuid, DataSource dataSource) {
        return insertTestVm(orionGuid, 1, dataSource, 1);
    }

    public static VirtualMachine insertTestVmCustomDc(UUID orionGuid, DataSource dataSource, int dataCenterId) {
        return insertTestVm(orionGuid, 1, dataSource, dataCenterId);
    }

    public static VirtualMachine insertTestVmWithIp(UUID orionGuid, DataSource dataSource) {
        VirtualMachine virtualMachine = insertTestVm(orionGuid, 1, dataSource, 1);
        return addIpToTestVm(dataSource, virtualMachine, 1);
    }

    public static VirtualMachine insertSecondaryIpToVm(UUID vmId, DataSource dataSource) {
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        return addIpToTestVm(dataSource, virtualMachineService.getVirtualMachine(vmId), 2);
    }

    public static Map<VirtualMachine, List<ScheduledJob>> insertTestVmWithScheduledBackup(UUID orionGuid,
                                                                                          DataSource dataSource) {
        VirtualMachine virtualMachine = insertTestVm(orionGuid, 1, dataSource, 1);
        List<ScheduledJob> scheduledJobs = addScheduledBackupToVm(dataSource, virtualMachine);
        Map vmJobMap = new HashMap<VirtualMachine, List<ScheduledJob>>();
        vmJobMap.put(virtualMachine, scheduledJobs);
        return vmJobMap;
    }

    public static VirtualMachine insertTestVm(UUID orionGuid, long vps4UserId, DataSource dataSource, int dataCenterId) {
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        long hfsVmId = getNextHfsVmId(dataSource);
        String sgidPrefix = "vps4-testing-" + Long.toString(hfsVmId) + "-";
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(
                vps4UserId, dataCenterId, sgidPrefix, orionGuid, "testVirtualMachine",
                10, 0, "centos-7");
        VirtualMachine virtualMachine = virtualMachineService.provisionVirtualMachine(params);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
        virtualMachine = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        return virtualMachine;
    }

    private static VirtualMachine addIpToTestVm(DataSource dataSource, VirtualMachine virtualMachine, int ipTypeId) {
        long hfsAddressId = getNextHfsAddressId(dataSource);
        Random random = new Random();
        String ipAddress =
                "192.168." + random.nextInt(255) + "." + random.nextInt(255);
        Sql.with(dataSource)
                .exec("INSERT INTO ip_address (hfs_address_id, ip_address, ip_address_type_id, vm_id)" +
                              " VALUES (?, ?::inet, ?, ?)",
                        null,
                        hfsAddressId, ipAddress, ipTypeId, virtualMachine.vmId);
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        return virtualMachineService.getVirtualMachine(virtualMachine.vmId);
    }

    private static List<ScheduledJob> addScheduledBackupToVm(DataSource dataSource, VirtualMachine virtualMachine) {
        UUID backupJobId = UUID.randomUUID();
        Sql.with(dataSource)
                .exec("INSERT INTO scheduled_job(id, vm_id, scheduled_job_type_id, created) VALUES (?, ?, 1, NOW())",
                        null,
                        backupJobId, virtualMachine.vmId);
        Sql.with(dataSource).exec("UPDATE virtual_machine SET backup_job_id = ? WHERE vm_id = ?", null,
                backupJobId, virtualMachine.vmId);
        ScheduledJobService scheduledJobService = new JdbcScheduledJobService(dataSource);
        return scheduledJobService.getScheduledJobs(virtualMachine.vmId);
    }

    public static void cleanupTestVmAndRelatedData(UUID vmId, DataSource dataSource) {
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);

        Sql.with(dataSource).exec("DELETE FROM vm_silenced_alert WHERE vm_id = ? ", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM hfs_vm_tracking_record WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM ip_address WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM vm_user WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM vm_action WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM snapshot_action sa USING snapshot s"
                + " WHERE sa.snapshot_id = s.id AND s.vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM snapshot WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM scheduled_job WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM monitoring_pf WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM imported_vm WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM project WHERE project_id = ?", null, vm.projectId);
        Sql.with(dataSource).exec("DELETE FROM scheduled_job WHERE scheduled_job_type_id=1 AND vm_id = ? ", null, vmId);
    }

    public static void deleteVps4User(long userId, DataSource dataSource) {
        Sql.with(dataSource).exec("DELETE FROM vps4_user where vps4_user_id = ?", null, userId);
    }

    public static void createActionWithDate(UUID vmId, ActionType actionType, Timestamp created, long userId,
                                            DataSource dataSource) {
        Sql.with(dataSource)
                .exec("INSERT INTO vm_action (vm_id, action_type_id, created, initiated_by) VALUES (?, ?, ?, ?)",
                        null, vmId, actionType.getActionTypeId(), created, "tester");
    }

    public static void insertTestNotification(UUID notificationId, NotificationType notificationType, boolean supportOnly,
                                              boolean dismissible, Instant start, Instant end, Instant validOn, Instant validUntil,
                                              List<NotificationFilter> filters, DataSource dataSource) {
        NotificationService notificationService = new JdbcNotificationService(dataSource);
        NotificationExtendedDetails notificationExtendedDetails = new NotificationExtendedDetails();
        notificationExtendedDetails.start = start;
        notificationExtendedDetails.end = end;
        notificationService.createNotification(notificationId, notificationType, supportOnly, dismissible,
                notificationExtendedDetails, filters, validOn, validUntil);
    }


    public static Long insertTestCustomNotes(UUID vmId, DataSource dataSource, String note, String user) {
        CustomNotesService customNotesService = new JdbcCustomNotesService(dataSource);
        return customNotesService.createCustomNote(vmId, note, user).id;
    }

    public static void cleanupTestCustomNotes(UUID vmId, DataSource dataSource) {
        CustomNotesService customNotesService = new JdbcCustomNotesService(dataSource);
        customNotesService.clearCustomNotes(vmId);
    }

    public static void cleanupTestNotification(UUID notificationId, DataSource dataSource) {
        NotificationService notificationService = new JdbcNotificationService(dataSource);
        notificationService.deleteNotification(notificationId);
    }

    public static void insertTestSnapshot(Snapshot snapshot, DataSource dataSource) {
        Sql.with(dataSource)
                .exec("INSERT INTO snapshot (id, hfs_image_id, project_id, hfs_snapshot_id, vm_id, name, status, " +
                                "snapshot_type_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        null, snapshot.id, snapshot.hfsImageId, snapshot.projectId, snapshot.hfsSnapshotId,
                        snapshot.vmId, snapshot.name, snapshot.status.getSnapshotStatusId(),
                        snapshot.snapshotType.getSnapshotTypeId());
    }

    public static void insertTestSnapshotAction(UUID snapshotId, ActionType actionType, ActionStatus statusType,
                                                DataSource dataSource) {
        Sql.with(dataSource).exec("INSERT  INTO snapshot_action"
                        + " (snapshot_id, action_type_id, initiated_by, status_id)"
                        + " VALUES (?, ?, ?, ?);",
                null, snapshotId, actionType.getActionTypeId(), "tester", statusType.ordinal() + 1);
    }
}
