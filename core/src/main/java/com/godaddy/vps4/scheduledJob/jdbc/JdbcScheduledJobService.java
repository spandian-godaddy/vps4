package com.godaddy.vps4.scheduledJob.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJob.ScheduledJobType;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.google.inject.Inject;

public class JdbcScheduledJobService implements ScheduledJobService {

    private final DataSource dataSource;
    
    @Inject
    public JdbcScheduledJobService(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    private ScheduledJob mapScheduledJob(ResultSet rs) throws SQLException {
        return new ScheduledJob(java.util.UUID.fromString(rs.getString("id")),
                java.util.UUID.fromString(rs.getString("vm_id")),
                ScheduledJob.ScheduledJobType.getById(rs.getInt("scheduled_job_type_id")),
                rs.getTimestamp("created", TimestampUtils.utcCalendar).toInstant());
    }
    
    @Override
    public List<ScheduledJob> getScheduledJobs(UUID vmId) {
        return Sql.with(dataSource).exec("SELECT * from scheduled_job WHERE vm_id=? ORDER BY created DESC", Sql.listOf(this::mapScheduledJob), vmId);
    }

    @Override
    public List<ScheduledJob> getScheduledJobsByType(UUID vmId, ScheduledJobType type) {
        return Sql.with(dataSource).exec("SELECT * from scheduled_job WHERE vm_id=? AND scheduled_job_type_id=? ORDER BY created DESC", Sql.listOf(this::mapScheduledJob), vmId, type.getId());
    }

    @Override
    public ScheduledJob getScheduledJob(UUID id) {
        return Sql.with(dataSource).exec("SELECT * from scheduled_job WHERE id=?", Sql.nextOrNull(this::mapScheduledJob), id);
    }

    @Override
    public void insertScheduledJob(UUID id, UUID vmId, ScheduledJobType type) {
        Sql.with(dataSource).exec("INSERT INTO scheduled_job (id, vm_id, scheduled_job_type_id) VALUES (?,?,?)", null, id, vmId, type.getId());
    }

}
