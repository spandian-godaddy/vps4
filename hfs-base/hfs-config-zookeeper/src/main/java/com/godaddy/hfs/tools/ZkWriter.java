package com.godaddy.hfs.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.io.Charsets;

/**
 *
 * @author dcomes
 */
public class ZkWriter {

    private static final Logger logger = LoggerFactory.getLogger(ZkWriter.class);

    private final CuratorFramework zkClient;

    private final Path basePath;
    private final Path envPath;
    private final String zkPath;

    public ZkWriter(CuratorFramework zkClient, Path basePath, Path envPath, String zkPath) {
        this.zkClient = zkClient;
        this.basePath = basePath;
        this.envPath = envPath;
        this.zkPath = zkPath;
    }

    protected String mergeConfig(Path config, Path...overrideConfigs) throws IOException {

        Properties prop = new Properties();
        try (InputStream basefis = Files.newInputStream(config)) {
            prop.load(basefis);

            for (Path envConfig : overrideConfigs) {
                if (Files.exists(envConfig)) {
                    logger.info("Using env override properties file: {}", envConfig);
                    try (InputStream envFis = Files.newInputStream(envConfig)) {
                        prop.load(envFis);
                    }
                } else {
                    logger.info("No env override properties file: {}", envConfig);
                }
            }
        }

        List<Path> configSources = new ArrayList<>();
        configSources.addAll(Arrays.asList(overrideConfigs));
        configSources.add(config);

        String comment = "# Loaded from: \n# " + configSources
                                            .stream()
                                            .map(file -> file.getParent().toString()).collect(Collectors.joining("\n# "));
        StringWriter sw = new StringWriter();
        sw.write(comment);
        sw.write("\n# ");
        sw.write(new Date().toString());
        sw.write('\n');

        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new IllegalStateException("Key must be a string: " + entry.getKey());
            }
            String key = (String)entry.getKey();
            String value = String.valueOf(entry.getValue());
            sw.write(key.trim());
            sw.write('=');
            sw.write(value.trim());
            sw.write('\n');
        }

        return sw.toString();
    }

    void setZnodeData(String zkPath, String data) throws Exception {
        setZnodeData(zkPath, data.getBytes(Charsets.UTF8));
    }

    void setZnodeData(String zkPath, byte[] data) throws Exception {
        Stat checkStat = zkClient.checkExists().forPath(zkPath);
        if (checkStat != null) {
            // setData
            Stat setDataStat = zkClient.setData().forPath(zkPath, data);
            if (setDataStat != null) {
                String out = String.format("%1$-8d %2$-8d %3$-8d", setDataStat.getVersion(), setDataStat.getDataLength(), setDataStat.getMtime());
                logger.trace(out);
            }
        } else {
            // create
            String s = zkClient.create().creatingParentsIfNeeded().forPath(zkPath, data);
            if (s != null) {
                logger.trace("result: {}", s);
            }
        }
    }

    void updateBaseConfig(Set<String> loadedZkPaths, String zkPath, Path baseDir) throws Exception {

        Files.list(baseDir).forEach( baseConf -> {

            try {
                String zkNodePath = zkPath + "/" + baseConf.getFileName();

                if (Files.isDirectory(baseConf)) {
                    updateBaseConfig(loadedZkPaths, zkNodePath, baseConf);
                } else {

                    // only upload files that haven't been uploaded before
                    if (loadedZkPaths.contains(zkNodePath)) {
                        logger.info("skipping base path (already loaded by environment): {}", zkNodePath);
                    } else {
                        logger.info("{} <= {}", zkNodePath, baseConf);
                        setZnodeData(zkNodePath, Files.readAllBytes(baseConf));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    void updateEnvConfig(Set<String> loadedZkPaths, String zkPath, Path baseDir, Path envDir) throws IOException {

       //logger.info("loading '{}' to zk path {}", configName, zkPath);


       // iterate through base directory
       // for every file, see if there is a corresponding 'environment' file
       // - if there is a corresponding environment file
       //   - if the file is a properties file, merge the two (env overriding) and write to znode
       //   - for all other files, write the 'env' file to a znode with the same name
       //
        logger.info("dir: {} <= ({} <- {})", zkPath, envDir, baseDir);

        Files.list(envDir).forEach( envConf -> {
           try {
               String zkNodePath = zkPath + "/" + envConf.getFileName();

               if (Files.isDirectory(envConf)) {
                   // recurse into the base and env directories
                   updateEnvConfig(loadedZkPaths, zkNodePath, envConf, baseDir.resolve(envConf.getFileName()));
               } else {

                   if (envConf.toString().endsWith(".properties")) {

                       // we found a configuration file
                       // merge with env properties (which may or not exist)
                       // and upload to znode

                       Path baseConf = baseDir.resolve(envConf.getFileName());
                       if (Files.exists(baseConf)) {
                           // override the base config with the env config
                           logger.info("merging {} <= ({} <- {})", zkNodePath, envConf, baseConf);
                           setZnodeData(zkNodePath, mergeConfig(baseConf, envConf));
                       } else {
                           // no base config, just write the env config
                           logger.info("using base config {} <= {}", zkNodePath, baseConf);
                           setZnodeData(zkNodePath, Files.readAllBytes(envConf));
                       }
                       loadedZkPaths.add(zkNodePath);

                   } else {
                       // we found a regular file (non-configuration)
                       // upload it directly to a znode
                       logger.info("{} <= {}", zkNodePath, envConf);
                       setZnodeData(zkNodePath, Files.readAllBytes(envConf));
                       loadedZkPaths.add(zkNodePath);
                   }
               }
           } catch (Exception e) {
               throw new RuntimeException(e);
           }
       });

    }

    public void initZKNode() throws IOException, Exception {

        Set<String> loadedZkPaths = new HashSet<>();

        // environment-specific configuration takes precedence
        // - upload any standalone files
        // - merge/override environment files to base configurations
        if (this.envPath != null && Files.exists(this.envPath)) {
            updateEnvConfig(loadedZkPaths, this.zkPath, this.basePath, this.envPath);
        }

        // upload whatever znodes weren't overridden by the
        // environment-specific config
        updateBaseConfig(loadedZkPaths, this.zkPath, this.basePath);
    }
}
