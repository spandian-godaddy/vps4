package com.godaddy.vps4.customNotes.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.customNotes.CustomNote;
import com.godaddy.vps4.customNotes.CustomNotesService;
import com.godaddy.vps4.util.TimestampUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class JdbcCustomNotesService implements CustomNotesService {
    private final DataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(JdbcCustomNotesService.class);
    private final String customNotesTableName = "vm_custom_notes";

    @Inject
    public JdbcCustomNotesService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<CustomNote> getCustomNotes(UUID vmId) {
        return Sql.with(dataSource).exec("SELECT id, vm_id, author, created, note " +
                        " FROM " + customNotesTableName +
                        " WHERE vm_id = ?",
                Sql.listOf(this::mapCustomNote), vmId);
    }

    @Override
    public CustomNote getCustomNote(long customNoteId) {
        return Sql.with(dataSource).exec("SELECT id, vm_id, author, created, note " +
                        " FROM " + customNotesTableName +
                        " WHERE id = ?",
                Sql.nextOrNull(this::mapCustomNote), customNoteId);
    }

    @Override
    public CustomNote createCustomNote(UUID vmId, String note, String author) {
        Long id = Sql.with(dataSource).exec("INSERT INTO " + customNotesTableName +
                        " (vm_id, author, note) VALUES (?, ?, ?) RETURNING id;", Sql.nextOrNull(rs -> rs.getLong("id")),
                vmId, author, note);
        return getCustomNote(id);
    }

    @Override
    public void clearCustomNotes(UUID vmId) {
        Sql.with(dataSource).exec("DELETE FROM " + customNotesTableName +
                        " WHERE vm_id = ?", null, vmId);
    }

    @Override
    public void deleteCustomNote(Long noteId) {
        Sql.with(dataSource).exec("DELETE FROM " + customNotesTableName +
                " WHERE id = ?", null, noteId);
    }

    private CustomNote mapCustomNote(ResultSet rs) throws SQLException {
        CustomNote customNote = new CustomNote();
        customNote.id = rs.getLong("id");
        customNote.vmId = UUID.fromString(rs.getString("vm_id"));
        customNote.author = rs.getString("author");
        customNote.created = rs.getTimestamp("created", TimestampUtils.utcCalendar).toInstant();
        customNote.note = rs.getString("note");
        return customNote;

    }
}
