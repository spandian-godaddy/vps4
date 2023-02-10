package com.godaddy.hfs.zookeeper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;

import org.junit.Test;
import static org.junit.Assert.*;

public class ZooKeeperClientTest {

    static final String SYSCONFIG_MATCH = "ZK_HOSTS=\"p3dlvps4zk01.cloud.phx3.gdg:2181,p3dlvps4zk02.cloud.phx3.gdg:2181,p3dlvps4zk03.cloud.phx3.gdg:2181,p3dlvps4zk04.cloud.phx3.gdg:2181\"";

    static final String SYSCONFIG_NOMATCH = "NOT_ZK_HOSTS=\"other\"";

    @Test
    public void testSysConfig() {

        Matcher matcher = ZooKeeperClient.ETC_PREFIX_PATTERN.matcher(SYSCONFIG_MATCH);

        assertTrue(matcher.matches());

        assertEquals(1, matcher.groupCount());
        assertEquals("p3dlvps4zk01.cloud.phx3.gdg:2181,p3dlvps4zk02.cloud.phx3.gdg:2181,p3dlvps4zk03.cloud.phx3.gdg:2181,p3dlvps4zk04.cloud.phx3.gdg:2181", matcher.group(1));
    }

    @Test
    public void testSysConfigNotMatches() {

        Matcher matcher = ZooKeeperClient.ETC_PREFIX_PATTERN.matcher(SYSCONFIG_NOMATCH);

        assertFalse(matcher.matches());
    }

    @Test
    public void testAsdf() throws Exception {
        Path path = Paths.get(getClass().getResource("/etc_zookeeper").toURI());
        assertEquals("p3dlvps4zk01.cloud.phx3.gdg:2181,p3dlvps4zk02.cloud.phx3.gdg:2181,p3dlvps4zk03.cloud.phx3.gdg:2181,p3dlvps4zk04.cloud.phx3.gdg:2181", ZooKeeperClient.readSysconfig(path));
    }
}
