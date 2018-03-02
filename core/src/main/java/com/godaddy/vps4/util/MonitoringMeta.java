package com.godaddy.vps4.util;

import com.godaddy.hfs.config.Config;
import com.google.inject.Inject;

public class MonitoringMeta {

    private final long accountId;
    private final String kafkaTopic;

    @Inject
    public MonitoringMeta(Config config)
    {
        accountId = Long.parseLong(config.get("monitoring.nodeping.account.id"));
        kafkaTopic = config.get("vps4.monitoring.kafka.topic");
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getNotificationTopic() {
        return kafkaTopic;
    }
}
