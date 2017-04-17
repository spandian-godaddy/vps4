package com.godaddy.vps4.consumer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.godaddy.vps4.handler.Vps4MessageHandler;
import com.godaddy.vps4.handler.Vps4MessageHandlerException;

public class Vps4Consumer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Vps4Consumer.class);

    private KafkaConfiguration kafkaConfig;

    private KafkaConsumer<String, String> kafkaConsumer;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    private Vps4MessageHandler messageHandler;

    @Inject
    public Vps4Consumer(KafkaConfiguration kafkaConfig, Vps4MessageHandler messageHandler) {
        this.kafkaConfig = kafkaConfig;
        this.messageHandler = messageHandler;
    }

    
    @Override
    public void run() {

        String threadName = Thread.currentThread().getName();
        logger.info("Starting consumer {} ...", threadName);

        try {

            List<String> topics = Arrays.asList(kafkaConfig.getTopic());
            logger.info("Topics: {}", topics);
            // Create a kafka consumer
            kafkaConsumer = new KafkaConsumer<>(kafkaConfig.getKafkaConsumerProps());
            // kafka consumer should subscribe to the topic
            kafkaConsumer.subscribe(topics);

            while (!closed.get()) {

                // poll the topic in a loop
                ConsumerRecords<String, String> records = kafkaConsumer.poll(200);

                for (ConsumerRecord<String, String> record : records) {

                    Map<String, Object> data = new HashMap<>();
                    data.put("partition", record.partition());
                    data.put("offset", record.offset());
                    data.put("value", record.value());

                    logger.info("Message retrieved: {}", record.value());
                    logger.info("Message handler {} will handle this message now... ", messageHandler.getClass().getName());
                    
                    // handle the message
                    messageHandler.handleMessage(record.value());
                }
            }
        }
        catch (WakeupException e) {
            // ignore for shutdown
            logger.info("Invoking shutdown of kafka consumer {}: {} ", threadName, e);
            if (!closed.get()) {
                throw e;
            }
        }
        catch( IllegalStateException | IllegalArgumentException | KafkaException ex ) {
            logger.error("Caught Kafka Exception: ", ex);
        }
        catch(Vps4MessageHandlerException vme) {
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
        // invoking shutdown of consumer
        String threadName = Thread.currentThread().getName();
        logger.info("Invoking shutdown of consumer {}...", threadName);
        closed.set(true);
        kafkaConsumer.wakeup();
    }
}
