package com.godaddy.vps4.consumer;


import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.godaddy.vps4.consumer.config.Vps4ConsumerConfiguration;
import com.godaddy.vps4.handler.MessageHandler;
import com.godaddy.vps4.handler.MessageHandlerException;

public class Vps4Consumer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Vps4Consumer.class);

    private final KafkaConfiguration kafkaConfig;

    private final MessageHandler messageHandler;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile KafkaConsumer<String, String> kafkaConsumer;

    @Inject
    public Vps4Consumer(Vps4ConsumerConfiguration consumerConfig) {
        this.kafkaConfig = consumerConfig.kafkaConfiguration;
        this.messageHandler = consumerConfig.messageHandler;
    }


    @Override
    public synchronized void run() {

        String threadName = Thread.currentThread().getName();
        logger.info("Starting consumer {} ...", threadName);

        try {

            logger.info("Topic: {}", kafkaConfig.getTopic());

            // Create a kafka consumer
            kafkaConsumer = new KafkaConsumer<>(kafkaConfig.getKafkaConsumerProps());

            // kafka consumer should subscribe to the topic
            kafkaConsumer.subscribe(Arrays.asList(kafkaConfig.getTopic()));

            while (!closed.get()) {

                // poll the topic in a loop
                ConsumerRecords<String, String> records = kafkaConsumer.poll(200);

                for (ConsumerRecord<String, String> record : records) {

                    logger.info("Message retrieved: {}", record.value());
                    logger.info("Message handler {} will handle this message now... ", messageHandler.getClass().getName());

                    // handle the message
                    messageHandler.handleMessage(record);

                    logger.info("offset: {}", record.offset());
                }
            }
        }
        catch (WakeupException e) {
            // ignore for shutdown
            if (!closed.get()) {
                logger.info("Consumer wakeup before close {}", threadName, e);
                throw e;
            }
        }
        catch( IllegalStateException | IllegalArgumentException | KafkaException ex ) {
            logger.error("Caught Kafka Exception: ", ex);
        }
        catch(MessageHandlerException vme) {
            logger.error("Message Handler could not handle message. Caught vps4 message exception: {}", vme);
        }
        catch (Exception ex) {
            logger.error("Caught exception : ", ex);
            throw ex;
        }
        finally {
            logger.info("Closing consumer {}", threadName);

            try {

                kafkaConsumer.close();

            } catch(InterruptException iex) {
                logger.error("Caught exception during kafka consumer close action : ", iex);
            }
        }
    }

    public void shutdown() {

        if (closed.compareAndSet(false, true)) {

            String threadName = Thread.currentThread().getName();
            logger.info("Invoking shutdown of consumer {}...", threadName);

            kafkaConsumer.wakeup();
        }
    }


}
