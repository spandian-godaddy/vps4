package com.godaddy.vps4.consumer;

import com.godaddy.vps4.consumer.config.Vps4ConsumerConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vps4ConsumerGroup {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ConsumerGroup.class);

    private final List<Vps4Consumer> consumers;

    public Vps4ConsumerGroup(List<Vps4Consumer> consumers) {
        this.consumers = Collections.unmodifiableList(consumers);
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


    public static Vps4ConsumerGroup build(List<Vps4ConsumerConfiguration> configs) {

        List<Vps4Consumer> consumers = new ArrayList<>();

        for (Vps4ConsumerConfiguration config : configs) {
            logger.info("creating consumer for config: {}", config.kafkaConfiguration);

            for (int i = 0; i < config.kafkaConfiguration.getNumberOfConsumers(); i++) {
                Vps4Consumer consumer = new Vps4Consumer(config);
                consumers.add(consumer);
            }
        }
        return new Vps4ConsumerGroup(consumers);
    }

}
