package com.godaddy.hfs.config;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

public class FileConfig {

    private static final Logger logger = LoggerFactory.getLogger(FileConfig.class);

    public ConfigNode readConfig(String resourcePath) throws IOException {

        //
        URL resource = FileConfig.class.getResource(resourcePath);
        if (resource == null) {
            logger.warn("resource not found: {}", resourcePath);
            return null;
        }

        URI uri = null;
        try {
            uri = resource.toURI();
        } catch (URISyntaxException e) {
            logger.warn("Invalid URI syntax", e);
            return null;
        }

        Path path = null;

        FileSystem fs = null;
        try {
            try {
                path = Paths.get(uri);

            } catch (FileSystemNotFoundException e) {
                fs = FileSystems.newFileSystem(uri, Collections.<String,Object>emptyMap());

                path = fs.provider().getPath(uri);

                fs.provider().newDirectoryStream(path, childPath -> true);
            }

            ConfigNode config = readConfig(path);

            return config;

        } finally {
            if (fs != null) {
                fs.close();
            }
        }


    }

    public ConfigNode readConfig(Path path) throws IOException {

        logger.trace("building config node: {}", path);

        if (path.getFileSystem().provider().readAttributes(path, BasicFileAttributes.class).isDirectory()) {

            return readDirectoryConfig(path);
        }
        return readFileConfig(path);
    }

    protected ConfigNode readDirectoryConfig(Path path) throws IOException {

        // recurse into directory

        try (DirectoryStream<Path> ds = path.getFileSystem().provider().newDirectoryStream(path, childPath -> true)) {

            String name = path.getName(path.getNameCount() - 1).toString();
            ConfigNode dirNode = new ConfigNode(name, null);

            for (Path childPath : ds) {
                ConfigNode childNode = readConfig(childPath);
                dirNode.getChildren().add(childNode);
            }
            return dirNode;
        }
    }

    public ConfigNode readFileConfig(Path path) throws IOException {

        String name = path.getName(path.getNameCount() - 1).toString();
        byte[] data = ByteStreams.toByteArray(path.getFileSystem().provider().newInputStream(path));

        return new ConfigNode(name, data);
    }

}
