package com.godaddy.vps4.phase3.producer;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vps4ProducerStub {
    
    private static final Logger logger = LoggerFactory.getLogger(Vps4ProducerStub.class);
    public final Properties props = new Properties();

    public void stubProducer() {
        
        props.put("bootstrap.servers", "p3dlvps4kafka01.cloud.phx3.gdg:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

    }
    
    public void produceTestData(int messageCount) {

        Producer<String, String> producer = new KafkaProducer<>(props);
        for(int i = 1; i <= messageCount; i++) {
            producer.send(new ProducerRecord<String, String>("vps4-test-topic", Integer.toString(i), Integer.toString(i)));
            logger.info("Produced message: {}", Integer.toString(i));
        }

        producer.close();

    }
}
