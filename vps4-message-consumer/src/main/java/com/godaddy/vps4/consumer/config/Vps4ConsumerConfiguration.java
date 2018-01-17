package com.godaddy.vps4.consumer.config;

import com.godaddy.vps4.handler.MessageHandler;

public class Vps4ConsumerConfiguration {

    public KafkaConfiguration kafkaConfiguration;
    public MessageHandler messageHandler;

    public Vps4ConsumerConfiguration() {

    }

    public Vps4ConsumerConfiguration(KafkaConfiguration kafkaConfig, MessageHandler messageHandler) {
        this.kafkaConfiguration = kafkaConfig;
        this.messageHandler = messageHandler;
    }
}
