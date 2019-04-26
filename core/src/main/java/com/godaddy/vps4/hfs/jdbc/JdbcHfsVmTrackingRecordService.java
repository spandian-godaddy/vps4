package com.godaddy.vps4.hfs.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.hfs.HfsVmTrackingRecord;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.util.TimestampUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public class JdbcHfsVmTrackingRecordService implements HfsVmTrackingRecordService {

    private final DataSource dataSource;

    @Inject
    public JdbcHfsVmTrackingRecordService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public HfsVmTrackingRecord get(long hfsVmId) {
        return Sql.with(dataSource).exec("SELECT * FROM hfs_vm_tracking_record WHERE hfs_vm_id=?",
                Sql.nextOrNull(this::mapHfsVm), hfsVmId);
    }

    @Override
    public HfsVmTrackingRecord create(long hfsVmId, UUID vmId, UUID orionGuid) {
        Sql.with(dataSource).exec("INSERT INTO hfs_vm_tracking_record (hfs_vm_id, vm_id, orion_guid) VALUES (?,?,?)",
                null, hfsVmId, vmId, orionGuid);
        return get(hfsVmId);
    }

    private HfsVmTrackingRecord mapHfsVm(ResultSet rs) throws SQLException {
        HfsVmTrackingRecord hfsVm = new HfsVmTrackingRecord();
        hfsVm.hfsVmId = rs.getLong("hfs_vm_id");
        hfsVm.vmId = UUID.fromString(rs.getString("vm_id"));
        hfsVm.orionGuid = UUID.fromString(rs.getString("orion_guid"));
        hfsVm.requested = rs.getTimestamp("requested", TimestampUtils.utcCalendar).toInstant();
        Timestamp createdTs = rs.getTimestamp("created", TimestampUtils.utcCalendar);
        hfsVm.created = (createdTs == null) ? null : createdTs.toInstant();
        Timestamp canceledTs = rs.getTimestamp("canceled", TimestampUtils.utcCalendar);
        hfsVm.canceled = (canceledTs == null) ? null : canceledTs.toInstant();
        Timestamp destroyedTs = rs.getTimestamp("destroyed", TimestampUtils.utcCalendar);
        hfsVm.destroyed = (destroyedTs == null) ? null : destroyedTs.toInstant();
        return hfsVm;
    }

    @Override
    public void setCreated(long hfsVmId) {
        Sql.with(dataSource).exec("UPDATE hfs_vm_tracking_record SET created=now_utc() WHERE hfs_vm_id = ?", null,
                hfsVmId);
    }

    @Override
    public void setCanceled(long hfsVmId) {
        Sql.with(dataSource).exec("UPDATE hfs_vm_tracking_record SET canceled=now_utc() WHERE hfs_vm_id = ?", null,
                hfsVmId);
    }

    @Override
    public void setDestroyed(long hfsVmId) {
        Sql.with(dataSource).exec("UPDATE hfs_vm_tracking_record SET destroyed=now_utc() WHERE hfs_vm_id = ?", null,
                hfsVmId);
    }

    @Override
    public List<HfsVmTrackingRecord> getCanceled() {
        return Sql.with(dataSource).exec(
                "select * from hfs_vm_tracking_record where canceled is not null and destroyed is null",
                Sql.listOf(this::mapHfsVm));
    }

    @Override
    public List<HfsVmTrackingRecord> getUnused() {
        return Sql.with(dataSource).exec(
                "select hfs.* from hfs_vm_tracking_record hfs left join virtual_machine vm on hfs.vm_id = vm.vm_id " +
                        "where hfs.hfs_vm_id <> vm.hfs_vm_id and hfs.created is not null and hfs.canceled is null " +
                        "and hfs.destroyed is null",
                Sql.listOf(this::mapHfsVm));
    }

    @Override
    public List<HfsVmTrackingRecord> getRequested() {
        return Sql.with(dataSource).exec(
                "select * from hfs_vm_tracking_record where requested is not null and created is null " +
                        "and canceled is null and destroyed is null",
                Sql.listOf(this::mapHfsVm));
    }
}
