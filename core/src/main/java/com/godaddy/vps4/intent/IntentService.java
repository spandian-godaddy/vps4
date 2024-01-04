package com.godaddy.vps4.intent;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.intent.model.Intent;

public interface IntentService {
    List<Intent> getIntents();
    List<Intent> getVmIntents(UUID vmId);
    List<Intent> setVmIntents(UUID vmId, List<Intent> intents);
}