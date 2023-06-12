package com.godaddy.vps4.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.zookeeper.ZooKeeperClient;
import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.godaddy.vps4.consumer.config.Vps4ConsumerConfiguration;
import com.godaddy.vps4.consumer.config.ZookeeperConfig;
import com.godaddy.vps4.handler.MessageHandler;
import com.godaddy.vps4.handler.util.ZkAppRegistrationService;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import ch.qos.logback.classic.Level;

public class Vps4ConsumerApplication {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ConsumerApplication.class);

    private final static CountDownLatch shutdownLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        Injector injector = Vps4ConsumerInjector.newInstance();

        setApplicationLogLevels(injector.getInstance(Config.class));

        boolean skipZkRegistration = Boolean.parseBoolean(System.getProperty("SkipZkRegistration"));

        if(skipZkRegistration){
            runVps4ConsumerGroup(injector);
        }
        else {
            ZookeeperConfig zkConfig = injector.getInstance(ZookeeperConfig.class);
            // get a handle to the zookeeper registration service
            ZkAppRegistrationService zkAppRegistrationService =
                    new ZkAppRegistrationService(zkConfig.getPath(), zkConfig.getServiceName(),
                            ZooKeeperClient.getInstance());

            runZkServiceRegistration(zkAppRegistrationService,
                    (() -> runVps4ConsumerGroup(injector)));
        }

    }

	private static void setApplicationLogLevels(Config config) {
		// Set the application log level.
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        Level level = Level.toLevel(config.get("vps4.log.level.messageConsumer"), Level.INFO);
        root.setLevel(level);

        root.warn("Log level set to {}", level.levelStr);

        // Added Ability to set log level for kafka packges to debug issues if any.
        ch.qos.logback.classic.Logger kafkaRootLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger("org.apache.kafka");
        Level kafkaLevel = Level.toLevel(config.get("vps4.log.level.kafka"), Level.INFO);
        kafkaRootLogger.setLevel(kafkaLevel);
	}

    private static void runVps4ConsumerGroup(Injector injector) {
        List<Vps4ConsumerConfiguration> configs = getVps4ConsumerConfigs(injector);

        // create kafka consumers and start listening to messages on the topic
        Vps4ConsumerGroup consumerGroup = Vps4ConsumerGroup.build(configs);

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

	private static List<Vps4ConsumerConfiguration> getVps4ConsumerConfigs(Injector injector) {
        Config config = injector.getInstance(Config.class);
        String[] consumerNames = config.get("vps4.kafka.consumer.names", "Account,Panopta").split(",");

        List<Vps4ConsumerConfiguration> configs = new ArrayList<>();
        for(String name : consumerNames){
            configs.add(getVps4ConsumerConfig(injector, name));
        }
		return configs;
	}

    private static Vps4ConsumerConfiguration getVps4ConsumerConfig(Injector injector, String name) {
        KafkaConfiguration kafkaConfig = injector.getInstance(Key.get(KafkaConfiguration.class, Names.named(name)));
        MessageHandler messageHandler = injector.getInstance(Key.get(MessageHandler.class, Names.named(name)));
        return new Vps4ConsumerConfiguration(kafkaConfig, messageHandler);
    }

    private static void runZkServiceRegistration(ZkAppRegistrationService zkAppRegistrationService, Runnable vps4ConsumerGroup) {

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
