package com.godaddy.vps4.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.godaddy.vps4.handler.MessageHandler;

public class Vps4ConsumerGroup {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ConsumerGroup.class);

    private KafkaConfiguration kafkaConfig;

    private final List<Vps4Consumer> consumers;

    private final MessageHandler messageHandler;

    public Vps4ConsumerGroup(KafkaConfiguration kafkaConfig, MessageHandler messageHandler, List<Vps4Consumer> consumers) {
        this.kafkaConfig = kafkaConfig;
        this.messageHandler = messageHandler;
        this.consumers = Collections.unmodifiableList(consumers);
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public KafkaConfiguration getKafkaConfig() {
        return kafkaConfig;
    }

    public List<Vps4Consumer> getConsumers() {
        return consumers;
    }

    public List<Future<?>> submit(ExecutorService pool) {
        return consumers.stream()
                .map(consumer -> pool.submit(consumer))
                .collect(Collectors.toList());
    }

    public void shutdown() {
        for (Vps4Consumer consumer : consumers) {
            consumer.shutdown();
        }
    }


    public static Vps4ConsumerGroup build(KafkaConfiguration kafkaConfig, MessageHandler messageHandler) {

        List<Vps4Consumer> consumers = new ArrayList<>();

        logger.info("creating consumer group for config: {}", kafkaConfig);

        for (int i=0; i<kafkaConfig.getNumberOfConsumers(); i++) {
            Vps4Consumer consumer = new Vps4Consumer(kafkaConfig, messageHandler);
            consumers.add(consumer);
        }
        return new Vps4ConsumerGroup(kafkaConfig, messageHandler, consumers);
    }

}
