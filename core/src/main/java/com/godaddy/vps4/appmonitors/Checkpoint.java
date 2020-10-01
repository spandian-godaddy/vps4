package com.godaddy.vps4.appmonitors;

import java.time.Instant;

public class Checkpoint {
    public enum Name {
        CREATES_WITHOUT_PANOPTA
    }

    public Name name;
    public Instant checkpoint;
}
