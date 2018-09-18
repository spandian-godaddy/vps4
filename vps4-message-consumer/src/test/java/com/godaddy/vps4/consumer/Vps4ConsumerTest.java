package com.godaddy.vps4.consumer;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.godaddy.vps4.consumer.config.KafkaConfiguration;
import com.godaddy.vps4.consumer.config.Vps4ConsumerConfiguration;
import com.godaddy.vps4.handler.MessageHandler;
import com.godaddy.vps4.handler.MessageHandlerException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Vps4ConsumerTest {
    private int maxPolls = 2;
    private Vps4Consumer consumer;
    private ExecutorService pool;
    private MessageHandler messageHandler;
    private KafkaConsumer<String, String> mockKafkaConsumer;
    private Map<TopicPartition, List<ConsumerRecord<String, String>>> dummyPartitionRecords;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private boolean testShutdown = false;

    @Before
    public void setUp() {
        consumer = setupVps4Consumer();
        pool = Executors.newSingleThreadExecutor();
    }

    private Vps4Consumer setupVps4Consumer() {
        setupMockKafkaConsumer();
        messageHandler = mock(MessageHandler.class);

        Vps4ConsumerConfiguration consumerConfig = mock(Vps4ConsumerConfiguration.class);
        consumerConfig.kafkaConfiguration = mock(KafkaConfiguration.class);
        consumerConfig.messageHandler = messageHandler;

        return new Vps4Consumer(consumerConfig) {

            @Override
            protected void initKafkaConsumer() {
                this.setKafkaConsumer(mockKafkaConsumer);
            }
        };
    }

    private void setupMockKafkaConsumer() {
        mockKafkaConsumer = mock(KafkaConsumer.class);

        // Set up the records that'll be returned from a successful call to KafkaConsumer.poll
        dummyPartitionRecords = new HashMap<>();
        dummyPartitionRecords.put(
            new TopicPartition("topic-a", 0),
            Arrays.asList(new ConsumerRecord[] {
                new ConsumerRecord<>("topic-a", 0, 10, "key-1", "value-1"),
                new ConsumerRecord<>("topic-a", 0, 11, "key-2", "value-2"),
                new ConsumerRecord<>("topic-a", 0, 12, "key-3", "value-3")
            })
        );
        dummyPartitionRecords.put(
            new TopicPartition("topic-a", 1),
            Arrays.asList(new ConsumerRecord[] {
                    new ConsumerRecord<>("topic-a", 1, 10, "key-1", "value-1"),
                    new ConsumerRecord<>("topic-a", 1, 11, "key-2", "value-2"),
                    new ConsumerRecord<>("topic-a", 1, 12, "key-3", "value-3")
            })
        );
        dummyPartitionRecords.put(
            new TopicPartition("topic-a", 2),
            Arrays.asList(new ConsumerRecord[] {
                    new ConsumerRecord<>("topic-a", 2, 10, "key-1", "value-1"),
                    new ConsumerRecord<>("topic-a", 2, 11, "key-2", "value-2"),
                    new ConsumerRecord<>("topic-a", 2, 12, "key-3", "value-3")
            })
        );

        ConsumerRecords<String, String> consumerRecords = mock(ConsumerRecords.class);
        when(consumerRecords.partitions())
                .thenReturn(dummyPartitionRecords.keySet());
        for (TopicPartition partition: dummyPartitionRecords.keySet()) {
            when(consumerRecords.records(partition))
                    .thenReturn(dummyPartitionRecords.get(partition));
        }

        // Setup the return values for calls to KafkaConsumer.poll
        when(mockKafkaConsumer.poll(anyLong()))
            .thenAnswer(new Answer<ConsumerRecords<String, String>>() {
                private int count;

                @Override
                public ConsumerRecords<String, String> answer(InvocationOnMock invocationOnMock) throws Throwable {
                    count++;
                    if (count < maxPolls) {
                        return consumerRecords;
                    }
                    else {
                        if (testShutdown) {
                            consumer.shutdown();
                            shutdownLatch.countDown();
                            return consumerRecords;
                        }
                        else {
                            // Count down the value of the latch so our test will progress
                            shutdownLatch.countDown();
                            throw new WakeupException();
                        }
                    }
                }
        });
    }

    @Test
    public void callsKafkaConsumerPoll() throws Exception {
        maxPolls = 10;
        pool.submit(consumer);
        pool.shutdown();
        shutdownLatch.await();

        // we set up the mock kafka consumer to throw a Wakeup exception after the maxPolls has exceeded.
        // so we assert here that we have called poll 'maxPolls' times.
        verify(mockKafkaConsumer, times(maxPolls)).poll(anyLong());
    }

    @Test
    public void callsMessageHandlerToProcessEachMessageReceived() throws Exception {
        pool.submit(consumer);
        pool.shutdown();
        shutdownLatch.await();

        for (TopicPartition partition: dummyPartitionRecords.keySet()) {
            for (ConsumerRecord<String, String> record : dummyPartitionRecords.get(partition)) {
                verify(messageHandler, times(1)).handleMessage(record);
            }
        }
    }

    @Test
    public void commitsOffsetToKafkaAfterProcessingRecordsPerPartition() throws Exception {
        pool.submit(consumer);
        pool.shutdown();
        shutdownLatch.await();

        for (TopicPartition partition: dummyPartitionRecords.keySet()) {
            // Its OffsetMetadata(13) because in our test setup we had offsets go from 10 - 12
            verify(mockKafkaConsumer, times(1))
                .commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(13)));
        }
    }

    @Test
    public void commitsOffsetsAsUsualIfMessageHandlerThrowsErrorsThatDonotRequireARetry() throws Exception {
        // the false to new MessageHandlerException indicates that the
        // exception doesn't require a retry of message processing
        doThrow(new MessageHandlerException(false, new RuntimeException()))
            .when(messageHandler).handleMessage(any(ConsumerRecord.class));
        pool.submit(consumer);
        pool.shutdown();
        shutdownLatch.await();

        for (TopicPartition partition: dummyPartitionRecords.keySet()) {
            // The way records are setup in this test (with offsets 10 -12), we should only call commit with value 13
            for (ConsumerRecord<String, String> record: dummyPartitionRecords.get(partition)) {
                verify(mockKafkaConsumer, times(0))
                    .commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(record.offset())));
            }

            verify(mockKafkaConsumer, times(1))
                .commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(13)));
        }
    }

    @Test
    public void resetsConsumedOffsetIfMessageHandlerThrowsErrorsThatRequireARetry() throws Exception {
        for (TopicPartition partition: dummyPartitionRecords.keySet()) {
            ConsumerRecord<String, String> errorRecord = dummyPartitionRecords.get(partition).get(1);
            // the true to new MessageHandlerException indicates that the
            // exception doesn't require a retry of message processing
            doThrow(new MessageHandlerException(true, new RuntimeException()))
                .when(messageHandler).handleMessage(eq(errorRecord));
        }
        pool.submit(consumer);
        pool.shutdown();
        shutdownLatch.await();

        for (TopicPartition partition: dummyPartitionRecords.keySet()) {
            ConsumerRecord<String, String> errorRecord = dummyPartitionRecords.get(partition).get(1);
            verify(mockKafkaConsumer, times(1))
                    .commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(errorRecord.offset())));
            verify(mockKafkaConsumer, times(1)).seek(partition, errorRecord.offset());
        }
    }

    @Test
    public void stopsPollingWhenServiceWasRequestedToShutdown() throws Exception {
        // set flag that'll call shutdown on the customer
        testShutdown = true;
        pool.submit(consumer);
        pool.shutdown();
        shutdownLatch.await();
        verify(mockKafkaConsumer, times(maxPolls)).poll(anyLong());
    }
}