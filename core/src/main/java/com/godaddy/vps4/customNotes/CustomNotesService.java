package com.godaddy.vps4.customNotes;

import java.util.List;
import java.util.UUID;

public interface CustomNotesService {
    List<CustomNote> getCustomNotes(UUID vmId);
    CustomNote getCustomNote(UUID vmId, long customNoteId);
    CustomNote createCustomNote(UUID vmId, String note, String author);
    void clearCustomNotes(UUID vmId);
    void deleteCustomNote(UUID vmId, Long noteId);
}
