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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.godaddy.vps4.Environment;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.MaxSizeConfig.MaxSizePolicy;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.properties.GroupProperty;
import com.hazelcast.zookeeper.ZookeeperDiscoveryProperties;
import com.hazelcast.zookeeper.ZookeeperDiscoveryStrategyFactory;

public class HazelcastProvider implements Provider<HazelcastInstance> {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastProvider.class);

    private final com.godaddy.hfs.config.Config vps4Config;

    @Inject
    public HazelcastProvider(com.godaddy.hfs.config.Config vps4Config) {
        this.vps4Config = vps4Config;
    }

    @Override
    public HazelcastInstance get() {

        return Hazelcast.getOrCreateHazelcastInstance(newConfig());

    }

    private static ObjectMapper newObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private Config newConfig() {

        Config config = new Config();
        config.setInstanceName("vps4");

        // configure serialization
        ObjectMapper mapper = newObjectMapper();

        GlobalSerializerConfig globalSerializerConfig = new GlobalSerializerConfig()
                    .setImplementation(new JsonSerializer(mapper))
                    .setOverrideJavaSerialization(false);

        SerializationConfig serializationConfig = new SerializationConfig()
                    .setGlobalSerializerConfig(globalSerializerConfig);

        config.setSerializationConfig(serializationConfig);

        // configure eviction policy per map
        for (String cacheName : new String[]{CacheName.API_JWT_TOKENS,
                                             CacheName.SERVER_USAGE,
                                             CacheName.CPANEL_ACCESS_HASH,
                                             CacheName.CPANEL_API_TOKEN,
                                             CacheName.MAIL_RELAY_HISTORY,
                                             CacheName.PANOPTA_METRIC_GRAPH}) {
            config.getMapConfig(cacheName)
                  .setEvictionPolicy(EvictionPolicy.LRU)
                  .setMaxSizeConfig(new MaxSizeConfig(10000, MaxSizePolicy.PER_NODE));
        }

        if(performServiceDiscovery()) {

            logger.info("Performing Service Discovery for hazelcast nodes in {} env.", getCurrentEnvironment());

            // configure discovery
            String zookeeperURL = vps4Config.get("vps4.zk.node");
            logger.info("vps4.zk.node: {}", zookeeperURL);
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            config.setProperty(GroupProperty.DISCOVERY_SPI_ENABLED.getName(), "true");

            DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(new ZookeeperDiscoveryStrategyFactory());
            discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(), zookeeperURL);
            discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_PATH.key(), "/service/registrations");
            discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), "vps4-" + getCurrentEnvironment() + "-hazelcast-cluster");
            config.getNetworkConfig().getJoin().getDiscoveryConfig().addDiscoveryStrategyConfig(discoveryStrategyConfig);

        } else {

            List<String> nodes = Arrays.asList(vps4Config.get("vps4.nodes").split(";"));
            logger.info("Using nodes in {} environment for hazelcast.", getCurrentEnvironment());
            logger.info("cluster nodes: {}", nodes);

            NetworkConfig networkConfig = config.getNetworkConfig();
            JoinConfig joinConfig = networkConfig.getJoin();
            joinConfig.getMulticastConfig().setEnabled(false);
            joinConfig.getTcpIpConfig()
                    .setEnabled(true)
                    .setMembers(nodes);

        }

        return config;
    }

    private String getCurrentEnvironment() {
        return vps4Config.get("vps4.env", Environment.LOCAL.getLocalName());
    }

    private boolean performServiceDiscovery() {
        // use service discovery for all environments except local.
        return Environment.valueOf(getCurrentEnvironment().toUpperCase()) != Environment.LOCAL;
    }
}
