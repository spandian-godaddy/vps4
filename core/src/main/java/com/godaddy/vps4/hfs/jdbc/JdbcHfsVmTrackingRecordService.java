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
import java.util.UUID;

public class JdbcHfsVmTrackingRecordService implements HfsVmTrackingRecordService {

    private final DataSource dataSource;

    @Inject
    public JdbcHfsVmTrackingRecordService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

	@Override
    public HfsVmTrackingRecord getHfsVm(long hfsVmId) {
        return Sql.with(dataSource).exec("SELECT * FROM hfs_vm_tracking_record WHERE hfs_vm_id=?", Sql.nextOrNull(this::mapHfsVm), hfsVmId);
    }

    @Override
    public HfsVmTrackingRecord createHfsVm(long hfsVmId, UUID vmId, UUID orionGuid) {
        Sql.with(dataSource).exec("INSERT INTO hfs_vm_tracking_record (hfs_vm_id, vm_id, orion_guid) VALUES (?,?,?)", null, hfsVmId, vmId, orionGuid);
        return getHfsVm(hfsVmId);
    }

    @Override
    public void setHfsVmCreated(long hfsVmId) {
        Sql.with(dataSource).exec("UPDATE hfs_vm_tracking_record SET created=now_utc() WHERE hfs_vm_id = ?", null, hfsVmId);
    }

    @Override
    public void setHfsVmCanceled(long hfsVmId) {
        Sql.with(dataSource).exec("UPDATE hfs_vm_tracking_record SET canceled=now_utc() WHERE hfs_vm_id = ?", null, hfsVmId);
    }

    @Override
    public void setHfsVmDestroyed(long hfsVmId) {
        Sql.with(dataSource).exec("UPDATE hfs_vm_tracking_record SET destroyed=now_utc() WHERE hfs_vm_id = ?", null, hfsVmId);
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
}
