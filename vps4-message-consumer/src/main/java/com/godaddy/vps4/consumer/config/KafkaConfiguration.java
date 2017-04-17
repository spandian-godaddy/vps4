package com.godaddy.vps4.consumer.config;

import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.godaddy.hfs.config.Config;

public class KafkaConfiguration {

    private String topic;

    private int numberOfConsumers;
    
    private Properties kafkaConsumerProps;
    
    @Inject
    public KafkaConfiguration(Config vps4Config) {
        this.topic = vps4Config.get("vps4.kafka.topic", "vps4-account-updates");
        this.numberOfConsumers = Integer.parseInt(vps4Config.get("vps4.kafka.consumer.count", "1"));
        
        kafkaConsumerProps = new Properties();
        kafkaConsumerProps.put("bootstrap.servers", vps4Config.get("vps4.kafka.bootstrap.servers", "localhost:9092"));
        kafkaConsumerProps.put("group.id", vps4Config.get("vps4.kafka.group.id", "vps4-consumer-group-01"));
        kafkaConsumerProps.put("enable.auto.commit", Boolean.parseBoolean(vps4Config.get("vps4.kafka.enable.auto.commit", "true")));
        kafkaConsumerProps.put("auto.commit.interval.ms", Integer.parseInt(vps4Config.get("vps4.kafka.auto.commit.interval.ms", "1000")));
        kafkaConsumerProps.put("session.timeout.ms", Integer.parseInt(vps4Config.get("vps4.kafka.session.timeout.ms", "30000")));
        kafkaConsumerProps.put("key.deserializer", vps4Config.get("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        kafkaConsumerProps.put("value.deserializer", vps4Config.get("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        kafkaConsumerProps.put("fetch.min.bytes", Integer.parseInt(vps4Config.get("vps4.kafka.fetch.min.bytes", "1")));
        kafkaConsumerProps.put("client.id", vps4Config.get("vps4.kafka.client.id", "vps4-client"));

    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setNumberOfConsumers(int numberOfConsumers) {
        this.numberOfConsumers = numberOfConsumers;
    }

    public String getTopic() {
        return this.topic;
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