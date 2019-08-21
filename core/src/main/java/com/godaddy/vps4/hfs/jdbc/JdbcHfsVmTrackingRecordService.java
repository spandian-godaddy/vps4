package com.godaddy.vps4.hfs.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.hfs.HfsVmTrackingRecord;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.util.TimestampUtils;

public class JdbcHfsVmTrackingRecordService implements HfsVmTrackingRecordService {

    private final DataSource dataSource;

    @Inject
    public JdbcHfsVmTrackingRecordService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public HfsVmTrackingRecord get(long hfsVmId) {
        return Sql.with(dataSource).exec("SELECT hv.*, p.vhfs_sgid FROM hfs_vm_tracking_record hv " +
                        "JOIN virtual_machine v ON hv.vm_id = v.vm_id " +
                        " JOIN project p ON v.project_id = p.project_id WHERE hv.hfs_vm_id=?",
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
        hfsVm.sgid = rs.getString("vhfs_sgid");
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
    public List<HfsVmTrackingRecord> getTrackingRecords(ListFilters listFilters) {
        StringBuilder sqlQuery = new StringBuilder();
        List<Object> args = new ArrayList<>();

        sqlQuery.append(
                "SELECT hv.*, p.vhfs_sgid FROM hfs_vm_tracking_record hv JOIN virtual_machine v ON hv.vm_id = v.vm_id" +
                        " JOIN project p ON v.project_id = p.project_id WHERE 1=1 ");

        if (listFilters.sgid != null) {
            sqlQuery.append(" AND p.vhfs_sgid = ?");
            args.add(listFilters.sgid);
        }

        if (listFilters.vmId != null) {
            sqlQuery.append(" AND v.vm_id = ?");
            args.add(listFilters.vmId);
        }

        if (listFilters.hfsVmId != 0) {
            sqlQuery.append(" AND hv.hfs_vm_id = ?");
            args.add(listFilters.hfsVmId);
        }

        if (listFilters.byStatus != null) {
            switch (listFilters.byStatus) {
                case UNUSED:
                    sqlQuery.append(" AND hv.hfs_vm_id <> v.hfs_vm_id AND hv.created IS NOT null" +
                            " AND hv.canceled IS null AND hv.destroyed IS null");
                    break;
                case CANCELED:
                    sqlQuery.append(" AND hv.canceled IS NOT null AND hv.destroyed IS null");
                    break;
                case REQUESTED:
                    sqlQuery.append(" AND hv.requested IS NOT null AND hv.created IS null " +
                            " AND hv.canceled IS null AND hv.destroyed IS null");
                    break;
            }
        }

        return Sql.with(dataSource).exec(sqlQuery.toString(), Sql.listOf(this::mapHfsVm), args.toArray());
    }
}
