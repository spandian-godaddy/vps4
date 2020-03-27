package com.godaddy.vps4.backupstorage.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.backupstorage.BackupStorageService;
import com.godaddy.vps4.util.TimestampUtils;

public class JdbcBackupStorageService implements BackupStorageService {
    private final DataSource dataSource;

    @Inject
    public JdbcBackupStorageService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private BackupStorageModel mapBackupStorage(ResultSet resultSet) throws SQLException {
        return new BackupStorageModel(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("vm_id")),
                resultSet.getString("ftp_server"),
                resultSet.getString("ftp_user"),
                resultSet.getTimestamp("valid_on", TimestampUtils.utcCalendar).toInstant(),
                resultSet.getTimestamp("valid_until", TimestampUtils.utcCalendar).toInstant()
        );
    }

    @Override
    public void createBackupStorage(UUID vmId) {
        Sql.with(dataSource).exec(
                "INSERT INTO backup_storage (vm_id) VALUES (?)",
                null, vmId);
    }

    @Override
    public void destroyBackupStorage(UUID vmId) {
        Sql.with(dataSource).exec(
                "UPDATE backup_storage SET valid_until = now_utc() WHERE vm_id = ? AND valid_until = 'infinity'",
                null, vmId);
    }

    @Override
    public BackupStorageModel getBackupStorage(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT * from backup_storage WHERE vm_id = ? AND valid_until = 'infinity'",
                Sql.nextOrNull(this::mapBackupStorage), vmId);
    }

    @Override
    public void setBackupStorage(UUID vmId, String ftpServer, String ftpUser) {
        Sql.with(dataSource).exec(
                "UPDATE backup_storage SET ftp_server = ?, ftp_user = ? WHERE vm_id = ? AND valid_until = 'infinity'",
                null, ftpServer, ftpUser, vmId);
    }
}
