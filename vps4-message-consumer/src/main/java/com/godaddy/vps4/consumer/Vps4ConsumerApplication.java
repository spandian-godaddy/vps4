package com.godaddy.vps4.consumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.godaddy.vps4.handler.MessageHandler;
import com.google.inject.Injector;

import ch.qos.logback.classic.Level;

public class Vps4ConsumerApplication {

    public static void main(String[] args) {

        // Added Ability to set log level for kafka pacakges to debug issues if any.
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.kafka");
        root.setLevel(Level.INFO);

        Injector injector = Vps4ConsumerInjector.newInstance();

        KafkaConfiguration kafkaConfig = injector.getInstance(KafkaConfiguration.class);

        MessageHandler messageHandler = injector.getInstance(MessageHandler.class);

        // create kafka consumers and start listening to messages on the topic
        Vps4ConsumerGroup consumerGroup = Vps4ConsumerGroup.build(kafkaConfig, messageHandler);

        ExecutorService pool = Executors.newCachedThreadPool();

        consumerGroup.submit(pool);

        pool.shutdown();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            // notify the consumers they should stop work
            consumerGroup.shutdown();

            // no need to wait for the pool to terminate, since
            // we're already in a shutdown hook
            // (as soon as the pool terminates it the JVM stops)
        }));
    }

}
