package com.godaddy.vps4.consumer;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
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
    private static final long POLL_TIMEOUT = Long.MAX_VALUE; // milliseconds

    private final KafkaConfiguration kafkaConfig;

    private final MessageHandler messageHandler;

    private final Map<Integer, Long> commitedOffsets;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile KafkaConsumer<String, String> kafkaConsumer;

    protected void setKafkaConsumer(KafkaConsumer<String, String> kafkaConsumer) {
        this.kafkaConsumer = kafkaConsumer;
    }

    public static class AbortProcessingException extends RuntimeException {
    }


    @Inject
    public Vps4Consumer(Vps4ConsumerConfiguration consumerConfig) {
        this.kafkaConfig = consumerConfig.kafkaConfiguration;
        this.messageHandler = consumerConfig.messageHandler;
        this.commitedOffsets = new HashMap<>();
    }


    @Override
    public synchronized void run() {
        String threadName = Thread.currentThread().getName();
        logger.info("Starting consumer {} ...", threadName);

        try {
            initKafkaConsumer();

            while (!wasServiceRequestedToShutdown()) {
                logger.info("Fetching messages in consumer {} from topic {}...", threadName, kafkaConfig.getTopic());
                ConsumerRecords<String, String> records = kafkaConsumer.poll(POLL_TIMEOUT);
                processRecords(records);
            }
        }
        catch (WakeupException e) {
            // ignore for shutdown
            if (!wasServiceRequestedToShutdown()) {
                logger.info("Consumer wakeup before close {}", threadName, e);
                throw e;
            }
        }
        catch( IllegalStateException | IllegalArgumentException | KafkaException ex ) {
            logger.error("Caught Kafka Exception: ", ex);
        }
        catch (InterruptedException ex) {
            logger.error("Thread [{}] interrupted by {}", threadName, ex);
            Thread.currentThread().interrupt(); // reset the interruption status

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
                Thread.currentThread().interrupt(); // reset the interruption status
            }
        }
    }

    private void processRecords(ConsumerRecords<String, String> records) throws InterruptedException {
        for (TopicPartition partition : records.partitions()) {
            List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);

            logger.info("Processing records from [Topic: {}, Partition: {}]", partition.topic(), partition.partition());
            try {
                for (ConsumerRecord<String, String> record : partitionRecords) {
                    if (!processRecord(record)) {
                        // In case there is an error then:
                        // 1. Commit the records that have been processed successfully to this point
                        // 2. Seek to offset that failed so the next poll can fetch from there
                        commitProcessedRecordsAndResetConsumedOffset(partition, record);
                        throw new AbortProcessingException();
                    }
                }

                // Commit all records in the batch for the partition
                long nextOffset = partitionRecords.get(partitionRecords.size() - 1).offset() + 1;
                commitOffsetToKafka(nextOffset, partition);
            }
            catch (AbortProcessingException ex) {
                // do nothing, move on to the next partition
            }
        }
    }

    private void commitProcessedRecordsAndResetConsumedOffset(
        TopicPartition partition, ConsumerRecord<String, String> record) {
        commitOffsetToKafka(record.offset(), partition);
        resetConsumedOffset(partition, record);
    }

    private void resetConsumedOffset(TopicPartition partition, ConsumerRecord<String, String> record) {
        logger.info("Seek to offset [Partition: {}, offset: {}]", partition, record.offset());
        kafkaConsumer.seek(partition, record.offset());
    }

    private void commitOffsetToKafka(long nextOffset, TopicPartition partition) {
        if (shouldCommit(partition.partition(), nextOffset)) {
            logger.info("Committing offset [Partition: {}, offset: {}]", partition, nextOffset);
            kafkaConsumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(nextOffset)));
            commitedOffsets.put(partition.partition(), nextOffset);
        }
        else {
            logger.info("Skip committing offset [Partition: {}, offset: {}]. Already committed", partition, nextOffset);
        }
    }

    private boolean shouldCommit(int partition, long offset) {
        // if we haven't recorded an offset for this partition or if the recorded offset is different
        return !commitedOffsets.containsKey(partition) || commitedOffsets.get(partition) != offset;

    }

    // The semantics of this method is that it either returns success (if message was processed without error
    // or error was squelched) or it returns a failure (if it doesn't make sense to squelch an error)
    private boolean processRecord(ConsumerRecord<String, String> record) {
        logger.info("Processing record [Topic: {}, Partition: {}, offset: {}]",
            record.topic(), record.partition(), record.offset());
        logger.info("Message retrieved: {}", record.value());
        logger.info("Message handler {} will handle this message now... ", messageHandler.getClass().getName());

        try {
            messageHandler.handleMessage(record);
        }
        catch (MessageHandlerException ex) {
            if (ex.shouldRetry()) {
                // If the exception thrown indicates that a message should be retried then return false
                logger.error("Message handling error. Will retry. {}", ex);
                return false;
            }
            else {
                // squelch every other exception and signal message processing
                // as success via default return value from method
                logger.error("Message handling error. Will not retry. {}", ex);
            }
        }

        return true;
    }

    protected void initKafkaConsumer() {
        logger.info("Topic: {}", kafkaConfig.getTopic());
        kafkaConsumer = new KafkaConsumer<>(kafkaConfig.getKafkaConsumerProps());
        kafkaConsumer.subscribe(Arrays.asList(kafkaConfig.getTopic()));
    }

    private boolean wasServiceRequestedToShutdown() {
        return closed.get();
    }

    public void shutdown() {

        if (closed.compareAndSet(false, true)) {

            String threadName = Thread.currentThread().getName();
            logger.info("Invoking shutdown of consumer {}...", threadName);

            kafkaConsumer.wakeup();
        }
    }


}
