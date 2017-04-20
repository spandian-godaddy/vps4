package com.godaddy.vps4.consumer;

import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.godaddy.vps4.handler.MessageHandler;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Vps4ConsumerManager {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ConsumerManager.class);
    
    private KafkaConfiguration kafkaConfig;

    private ExecutorService consumerPool;

    private List<Future<?>> consumerFutures;

    @Inject
    public Vps4ConsumerManager(KafkaConfiguration kafkaConfig) {
        this.kafkaConfig = kafkaConfig;
    }

    @Inject
    public void createConsumerGroup(MessageHandler messageHandler) {

        int consumerCount = kafkaConfig.getNumberOfConsumers();
        logger.info("Creating consumer graup with {} consumer(s)", consumerCount);
        
        // ensure threads in the pool will have a relevant name
        ThreadFactory vps4Consumers = new ThreadFactoryBuilder().setNameFormat("Vps4Consumer-%d").build();
        consumerPool = Executors.newFixedThreadPool(consumerCount, vps4Consumers);

        final List<Vps4Consumer> consumers = new ArrayList<>();
        consumerFutures = new ArrayList<>(consumerCount);
        for (int i = 0; i < consumerCount; i++) {
            Vps4Consumer consumer = new Vps4Consumer(kafkaConfig, messageHandler);
            consumers.add(consumer);
            Future<?> future = consumerPool.submit(consumer);
            consumerFutures.add(future);
        }
    }

    public KafkaConfiguration getKafkaConfig() {
        return kafkaConfig;
    }

    public List<Future<?>> getConsumerFutures() {
        return consumerFutures;
    }

    public void gracefullyShutDownConsumerGroup() {
        gracefullyShutDownConsumerGroup(false, 30);
    }

    public void gracefullyShutDownConsumerGroup(boolean forceShutdown, int timeoutSeconds) {

        if(forceShutdown) {
            consumerPool.shutdownNow();
            return;
        }
        
        // Shutdown the pool, stop accepting any new tasks
        consumerPool.shutdown();
        
        try {
            // Wait a while for existing tasks to terminate
            if (!consumerPool.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                consumerPool.shutdownNow(); // Cancel currently executing tasks
            }
        }
        catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            consumerPool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

}
