package com.godaddy.vps4.consumer;

import ch.qos.logback.classic.Level;
import com.godaddy.hfs.zookeeper.ZooKeeperClient;
import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.godaddy.vps4.consumer.config.ZookeeperConfig;
import com.godaddy.vps4.handler.MessageHandler;
import com.godaddy.vps4.handler.util.ZkAppRegistrationService;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class Vps4ConsumerApplication {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ConsumerApplication.class);

    private final static CountDownLatch shutdownLatch = new CountDownLatch(1);

    public static void main(String[] args) {

        // Added Ability to set log level for kafka pacakges to debug issues if any.
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.kafka");
        root.setLevel(Level.INFO);

        Injector injector = Vps4ConsumerInjector.newInstance();


        ZookeeperConfig zkConfig = injector.getInstance(ZookeeperConfig.class);
        // get a handle to the zookeeper registration service
        ZkAppRegistrationService zkAppRegistrationService =
                new ZkAppRegistrationService(zkConfig.getPath(), zkConfig.getServiceName(),
                        ZooKeeperClient.getInstance());

        runZkServiceRegistration(zkAppRegistrationService,
                (() -> runVps4ConsumerGroup(injector)));

    }

    private static void runVps4ConsumerGroup(Injector injector) {
        // get the kafka configuration
        KafkaConfiguration kafkaConfig = injector.getInstance(KafkaConfiguration.class);

        MessageHandler messageHandler = injector.getInstance(MessageHandler.class);

        // create kafka consumers and start listening to messages on the topic
        Vps4ConsumerGroup consumerGroup = Vps4ConsumerGroup.build(kafkaConfig, messageHandler);

        ExecutorService pool = Executors.newCachedThreadPool();

        consumerGroup.submit(pool);

        logger.info("shutting down consumer group pool");
        pool.shutdown();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            // notify the consumers they should stop work
            consumerGroup.shutdown();

            // Force the shutdown hook to wait till all threads have completed.
            // This allows for the zk registration service to un-register itself.
            try {
                shutdownLatch.await();
            } catch (InterruptedException e) {
                logger.warn("interrupted waiting for shutdown latch to trigger");
            }

        }));

        logger.info("waiting on vps4 consumer group pool to terminate");

        while (!pool.isTerminated()) {
            try {
                pool.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.info("Vps4ConsumerGroup pool termination was interrupted. ", e);
            }
        }
    }

    public static void runZkServiceRegistration(ZkAppRegistrationService zkAppRegistrationService, Runnable vps4ConsumerGroup) {

        // register with zookeeper
        zkAppRegistrationService.register();

        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> future = pool.submit(vps4ConsumerGroup);

        try {
            future.get();
        } catch (InterruptedException e) {
            logger.warn("Caught InterruptedException exception while getting future: ", e);
        } catch (ExecutionException e) {
            logger.warn("Caught execution exception: ", e);
        } finally {
            try {
                logger.info("Un-registering with zookeeper...");
                zkAppRegistrationService.close();
                pool.shutdown();
            } finally {
                shutdownLatch.countDown();
            }
        }

    }

}
