package com.godaddy.vps4.phase3.consumer;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.godaddy.vps4.consumer.Vps4ConsumerInjector;
import com.godaddy.vps4.consumer.Vps4ConsumerManager;
import com.godaddy.vps4.handler.BasicMessageHandler;
import com.godaddy.vps4.phase3.producer.Vps4ProducerStub;
import com.google.inject.Injector;

/**
 * This test case can be run only of the host has a kafka broker running on it.
 * It simply produces a few messages using a producer thread, and attempts to see if they were consumed by the consumer thread.
 * 
 */
@Ignore
public class Vps4KafkaConsumerTest {

    Vps4ConsumerManager manager;
    BasicMessageHandler basicMsgHandler;

    public final class ConsumerThread extends Thread {
        @Override
        public void run() {
            Injector injector = Vps4ConsumerInjector.newInstance();
            // create kafka consumers and start listening to messages on the topic
            basicMsgHandler = new BasicMessageHandler();
            manager = injector.getInstance(Vps4ConsumerManager.class);
            manager.getKafkaConfig().setTopic("vps4-test-topic");
            manager.getKafkaConfig().setNumberOfConsumers(1);
            manager.createConsumerGroup(basicMsgHandler);
        }
    }

    public final class ProducerThread extends Thread {
        @Override
        public void run() {
            Vps4ProducerStub stub = new Vps4ProducerStub();
            stub.stubProducer();
            stub.produceTestData(5);
        }
    }

    @After
    public void tearDown() throws Exception {
        manager.gracefullyShutDownConsumerGroup(false, 3);
        basicMsgHandler = null;
        manager = null;
    }

    @Test
    public void testConsume() throws InterruptedException {

        // TODO: rigorous test goes here

        Thread consumerThread = new ConsumerThread();
        consumerThread.start();

        consumerThread.join(5000);

        Thread producerThread = new ProducerThread();
        producerThread.start();

        producerThread.join();
        
        int waitTimeInSecs = 0, maxWaitTimeSecs = 40;
        
        while(waitTimeInSecs < maxWaitTimeSecs && basicMsgHandler.getMessageCount() < 5 ) {
            Thread.sleep(1000);
            waitTimeInSecs += 1;
        }
        
        assertTrue("Produced message count does not match actual consumed message count. ", basicMsgHandler.getMessageCount() == 5);

    }

}
