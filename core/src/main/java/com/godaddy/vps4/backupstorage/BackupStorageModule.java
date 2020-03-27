package com.godaddy.vps4.backupstorage;

import com.godaddy.vps4.backupstorage.jdbc.JdbcBackupStorageService;
import com.google.inject.AbstractModule;

public class BackupStorageModule extends AbstractModule {
    @Override
    public void configure() {
        bind(BackupStorageService.class).to(JdbcBackupStorageService.class);
    }
}
