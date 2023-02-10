package com.godaddy.hfs.config;

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClasspathConfigBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ClasspathConfigBuilder.class);

    final String[] paths;

    public ClasspathConfigBuilder(String...paths) {
        this.paths = paths;
    }

    public void build(List<ConfigNode> nodes) {

        for (String path : paths) {
            ConfigNode node = buildConfig(path);
            if (node != null) {
                nodes.add(node);
            }
        }
    }

    protected ConfigNode buildConfig(String classpathPath) {
        URL configResource = ClasspathConfigBuilder.class.getResource(classpathPath);
        if (configResource != null) {

            try {
                URI uri = configResource.toURI();
                Path basePath = null;
                try {
                    basePath = Paths.get(uri);
                } catch (FileSystemNotFoundException e) {
                    FileSystem fs = FileSystems.newFileSystem(uri, Collections.<String,Object>emptyMap());

                    basePath = fs.provider().getPath(uri);
                }

                if (Files.exists(basePath)) {

                    logger.info("reading config at base path: {}", basePath);

                    FileConfig fileConfig = new FileConfig();

                    // read configs, renaming the root node to "" so that the root of the path
                    // isn't "base" or "dev/test/..."
                    ConfigNode baseNode = fileConfig.readConfig(basePath).rename("");

                    return baseNode;

                } else {
                    logger.info("No config found at path: {}", basePath);
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            logger.info("No config found on classpath at: {}", classpathPath);
        }
        return null;
    }
}
