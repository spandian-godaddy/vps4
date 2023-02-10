package com.godaddy.hfs.tools;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.curator.framework.CuratorFramework;

import com.godaddy.hfs.zookeeper.ZooKeeperClient;

public class ZkInit {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println("usage: cmd zkPath baseConfigPath envConfigPath");
            return;
        }

        String zkPath = args[0];
        Path basePath = Paths.get(args[1]);
        Path envPath = args.length > 2 ? Paths.get(args[2]) : null;

        CuratorFramework zk = ZooKeeperClient.getInstance();

        new ZkWriter(zk,
                basePath,
                envPath,
                zkPath).initZKNode();

        zk.close();
    }
}
