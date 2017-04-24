package com.godaddy.vps4.phase3.consumer;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.godaddy.vps4.consumer.Vps4ConsumerGroup;
import com.godaddy.vps4.consumer.Vps4ConsumerInjector;
import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.godaddy.vps4.handler.BasicMessageHandler;
import com.godaddy.vps4.phase3.producer.Vps4ProducerStub;
import com.google.inject.Injector;

/**
 * This test case can be run only of the host has a kafka broker running on it.
 * It simply produces a few messages using a producer thread, and attempts to see if they were consumed by the consumer thread.
 *
 */
//@Ignore
public class Vps4KafkaConsumerTest {

    @Test
    public void testConsume() throws Exception {

        // TODO: rigorous test goes here

        Injector injector = Vps4ConsumerInjector.newInstance();
        // create kafka consumers and start listening to messages on the topic

        KafkaConfiguration kafkaConfig = injector.getInstance(KafkaConfiguration.class);
        kafkaConfig.setNumberOfConsumers(1);
        kafkaConfig.setTopic("vps4-test-topic");

        ExecutorService pool = Executors.newCachedThreadPool();

        BasicMessageHandler messageHandler = new BasicMessageHandler();

        // create the consumer group
        Vps4ConsumerGroup consumerGroup = Vps4ConsumerGroup.build(kafkaConfig, messageHandler);

        consumerGroup.submit(pool);

        // spin off producer
        Future<?> producerFuture = pool.submit(() -> {
            Vps4ProducerStub stub = new Vps4ProducerStub();
            stub.produceTestData(5);
        });

        // wait until all the data has been produced
        producerFuture.get();

        Instant timeoutAt = Instant.now().plusSeconds(40);

        while( Instant.now().isBefore(timeoutAt)
            && messageHandler.getMessageCount() < 5 ) {

            Thread.sleep(1000);
        }

        // we either have the messages, or we've timed out...
        // either way, shut down the consumer group
        consumerGroup.shutdown();

        pool.shutdown();

        // wait until we timeout or the pool terminates (consumers cleanly shutting down)
        while (Instant.now().isBefore(timeoutAt) && !pool.isTerminated()) {
            pool.awaitTermination(1, TimeUnit.SECONDS);
        }

        if (!pool.isTerminated()) {
            pool.shutdownNow();
        }

        assertEquals(   "Produced message count does not match actual consumed message count. ",
                        5, messageHandler.getMessageCount());

    }

}
