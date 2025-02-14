package com.godaddy.vps4.consumer.config;

import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.godaddy.hfs.config.Config;

public abstract class KafkaConfiguration {
    private String topic;

    private int numberOfConsumers;
    
    private Properties kafkaConsumerProps;
    
    @Inject
    public KafkaConfiguration(Config vps4Config, String topicKey, String clientKey, String bootstrapServersKey) {
        kafkaConsumerProps = new Properties();

        topic = vps4Config.get(topicKey);
        kafkaConsumerProps.put("client.id", vps4Config.get(clientKey));
        this.numberOfConsumers = Integer.parseInt(vps4Config.get("vps4.kafka.consumer.count", "1"));
        
        kafkaConsumerProps.put("bootstrap.servers", vps4Config.get(bootstrapServersKey, "p3dlkckafka01.cloud.phx3.gdg:9092"));
        kafkaConsumerProps.put("group.id", vps4Config.get("vps4.kafka.group.id", "vps4-consumer-group-01"));
        kafkaConsumerProps.put("enable.auto.commit", Boolean.parseBoolean(vps4Config.get("vps4.kafka.enable.auto.commit", "false")));
        kafkaConsumerProps.put("session.timeout.ms", Integer.parseInt(vps4Config.get("vps4.kafka.session.timeout.ms", "120000")));
        kafkaConsumerProps.put("max.poll.interval.ms", Integer.parseInt(vps4Config.get("vps4.kafka.max.poll.interval.ms", Integer.toString(Integer.MAX_VALUE))));
        kafkaConsumerProps.put("max.poll.records", Integer.parseInt(vps4Config.get("vps4.max.poll.records", "75")));
        kafkaConsumerProps.put("key.deserializer", vps4Config.get("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        kafkaConsumerProps.put("value.deserializer", vps4Config.get("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        kafkaConsumerProps.put("fetch.min.bytes", Integer.parseInt(vps4Config.get("vps4.kafka.fetch.min.bytes", "1")));

    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setNumberOfConsumers(int numberOfConsumers) {
        this.numberOfConsumers = numberOfConsumers;
    }

    public String getTopic() {
        return topic;
    }

    public int getNumberOfConsumers() {
        return this.numberOfConsumers;
    }
    
    public Properties getKafkaConsumerProps() {
        return this.kafkaConsumerProps;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
