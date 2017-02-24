package com.godaddy.vps4.cache;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.MaxSizeConfig.MaxSizePolicy;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastProvider implements Provider<HazelcastInstance> {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastProvider.class);

    final com.godaddy.hfs.config.Config vps4Config;

    @Inject
    public HazelcastProvider(com.godaddy.hfs.config.Config vps4Config) {
        this.vps4Config = vps4Config;
    }

    @Override
    public HazelcastInstance get() {

        List<String> nodes = Arrays.asList(vps4Config.get("vps4.nodes").split(";"));
        logger.info("cluster nodes: {}", nodes);

        return Hazelcast.getOrCreateHazelcastInstance(newConfig(nodes));
    }

    public Config newConfig(List<String> nodes) {
        Config config = new Config();
        config.setInstanceName("vps4");

        //
        // configure serialization
        //
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        GlobalSerializerConfig globalSerializerConfig = new GlobalSerializerConfig()
                    .setImplementation(new JsonSerializer(mapper))
                    .setOverrideJavaSerialization(false);

        SerializationConfig serializationConfig = new SerializationConfig()
                    .setGlobalSerializerConfig(globalSerializerConfig);

        config.setSerializationConfig(serializationConfig);

        //
        // configure eviction policy per map
        //
        config.getMapConfig(CacheName.CPANEL_ACCESSHASH)
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizeConfig(new MaxSizeConfig(10000, MaxSizePolicy.PER_NODE));

        config.getMapConfig(CacheName.VM_USAGE)
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizeConfig(new MaxSizeConfig(10000, MaxSizePolicy.PER_NODE));

        //
        // configure discovery
        //
        NetworkConfig networkConfig = config.getNetworkConfig();
        JoinConfig joinConfig = networkConfig.getJoin();

        joinConfig.getMulticastConfig().setEnabled(false);

        joinConfig.getTcpIpConfig()
                    .setEnabled(true)
                    .setMembers(nodes);

        return config;
    }
}
