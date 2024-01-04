package com.godaddy.vps4.intent.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.intent.IntentService;
import com.godaddy.vps4.intent.model.Intent;
import com.google.inject.Inject;

public class JdbcIntentService implements IntentService {
    private final DataSource dataSource;

    @Inject
    public JdbcIntentService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Intent> getIntents() {
        return Sql.with(dataSource).exec("SELECT id, name FROM intent", Sql.listOf(this::mapIntent));
    }

    private Intent mapIntent(ResultSet rs) throws SQLException {
        Intent intent = new Intent();
        intent.id = rs.getInt("id");
        intent.name = rs.getString("name");
        return intent;
    }

    @Override
    public List<Intent> getVmIntents(UUID vmId) {
        return Sql.with(dataSource).exec("SELECT vi.intent_id, i.name, vi.description " +
                                         "FROM vm_intent vi join intent i on vi.intent_id = i.id " +
                                         "WHERE vm_id = ?", 
                Sql.listOf(this::mapVmIntent), 
                vmId);
    }

    private Intent mapVmIntent(ResultSet rs) throws SQLException {
        Intent intent = new Intent();
        intent.id = rs.getInt("intent_id");
        intent.name = rs.getString("name");
        intent.description = rs.getString("description");
        return intent;
    }

    @Override
    public List<Intent> setVmIntents(UUID vmId, List<Intent> intents) {
        for (Intent vmIntent : intents) {
            Sql.with(dataSource).exec("INSERT INTO vm_intent (vm_id, intent_id, description) VALUES (?,?,?)", 
                                      null, 
                                      vmId, 
                                      vmIntent.id, 
                                      vmIntent.description);
        }
        return getVmIntents(vmId);
    }
}