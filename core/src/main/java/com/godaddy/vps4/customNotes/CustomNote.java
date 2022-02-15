package com.godaddy.vps4.customNotes;

import java.time.Instant;
import java.util.UUID;

public class CustomNote {
    public long id;
    public UUID vmId;
    public Instant created;
    public String note;
    public String author;
}