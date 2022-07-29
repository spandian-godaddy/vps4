package com.godaddy.vps4.oh.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.oh.OhBackupDataService;
import com.godaddy.vps4.oh.backups.OhBackupData;
import com.godaddy.vps4.util.TimestampUtils;

public class JdbcOhBackupDataService implements OhBackupDataService {
    private final DataSource dataSource;

    @Inject
    public JdbcOhBackupDataService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private OhBackupData ohBackupMapper(ResultSet rs) throws SQLException {
        OhBackupData data = new OhBackupData();
        data.backupId = UUID.fromString(rs.getString("oh_backup_id"));
        data.vmId = UUID.fromString(rs.getString("vm_id"));
        data.created = rs.getTimestamp("created", TimestampUtils.utcCalendar).toInstant();
        data.destroyed = rs.getTimestamp("destroyed", TimestampUtils.utcCalendar).toInstant();
        return data;
    }

    @Override
    public void createBackup(UUID ohBackupId, UUID vmId, String name) {
        String query = "INSERT INTO oh_backup (oh_backup_id, vm_id, name) VALUES (?, ?, ?)";
        Sql.with(dataSource).exec(query, null, ohBackupId, vmId, name);
    }

    @Override
    public void destroyBackup(UUID ohBackupId) {
        String query = "UPDATE oh_backup SET destroyed = now_utc() WHERE oh_backup_id = ?";
        Sql.with(dataSource).exec(query, null, ohBackupId);
    }

    @Override
    public int totalFilledSlots(UUID vmId) {
        String query = "SELECT count(*) FROM oh_backup WHERE vm_id = ? AND destroyed > now_utc()";
        return Sql.with(dataSource).exec(query, Sql.nextOrNull(rs -> rs.getInt("count")), vmId);
    }

    @Override
    public List<OhBackupData> getBackups(UUID vmId) {
        String query = "SELECT * FROM oh_backup WHERE vm_id = ? AND destroyed > now_utc()";
        return Sql.with(dataSource).exec(query, Sql.listOf(this::ohBackupMapper), vmId);
    }

    @Override
    public OhBackupData getOldestBackup(UUID vmId) {
        String query = "SELECT * FROM oh_backup WHERE vm_id = ? AND destroyed > now_utc() ORDER BY created LIMIT 1";
        return Sql.with(dataSource).exec(query, Sql.nextOrNull(this::ohBackupMapper), vmId);
    }
}
