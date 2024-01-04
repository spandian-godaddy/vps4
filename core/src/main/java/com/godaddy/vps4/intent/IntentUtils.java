package com.godaddy.vps4.intent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.godaddy.vps4.intent.model.Intent;

public class IntentUtils {

    private IntentUtils() {}
    
    public static Map<Integer, Intent> getIntentsMap(List<Intent> intents) {
        Map<Integer, Intent> intentsMap = new HashMap<>();
        for (Intent intent : intents) {
            intentsMap.put(intent.id, intent);
        }
        return intentsMap;
    }
}
