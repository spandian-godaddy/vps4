package com.godaddy.vps4.sysadmin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;

import com.godaddy.hfs.io.Charsets;

public class VmUsageParserTest {

    @Test
    public void testParseLinux() throws Exception {

        try (InputStream is = VmUsageParserTest.class.getResourceAsStream("usage_stats_linux.json")) {
            assertNotNull(is);

            JSONObject json = (JSONObject)JSONValue.parse(new InputStreamReader(is, Charsets.UTF8));

            VmUsageParser parser = new VmUsageParser();

            VmUsage usage = parser.parse(json);

            assertNotNull(usage);

            assertNotNull(usage.disk);
            assertEquals(8040L, usage.disk.mibUsed);
            assertEquals(32908L, usage.disk.mibAvail);

            assertNotNull(usage.cpu);
            assertEquals(1.23d, usage.cpu.userPercent, 0d);
            assertEquals(0.19d, usage.cpu.systemPercent, 0d);

            assertNotNull(usage.io);
            assertEquals(2.01d, usage.io.totalTps, 0d);
            assertEquals(1.21d, usage.io.readTps, 0d);
            assertEquals(0.8d, usage.io.writeTps, 0d);

            assertNotNull(usage.mem);
            assertEquals(251d, usage.mem.mibMemFree, 0d);
            assertEquals(1588d, usage.mem.mibMemUsed, 0d);
        }
    }

    @Test
    public void testParseWindows() throws Exception {

        try (InputStream is = VmUsageParserTest.class.getResourceAsStream("usage_stats_windows.json")) {
            assertNotNull(is);

            JSONObject json = (JSONObject)JSONValue.parse(new InputStreamReader(is, Charsets.UTF8));

            VmUsageParser parser = new VmUsageParser();

            VmUsage usage = parser.parse(json);

            assertNotNull(usage);

            assertNotNull(usage.disk);
            assertEquals(49826L, usage.disk.mibUsed);
            assertEquals(61437L, usage.disk.mibAvail);

            assertNotNull(usage.cpu);
            assertEquals(0.47919d, usage.cpu.userPercent, 0.001d);
            assertEquals(0.67711d, usage.cpu.systemPercent, 0.001d);

            assertNotNull(usage.io);
            assertEquals(0.6466d, usage.io.totalTps, 0.0001d);
            assertEquals(0d, usage.io.readTps, 0d);
            assertEquals(0.6466d, usage.io.writeTps, 0.0001d);

            assertNotNull(usage.mem);
            assertEquals(3507d, usage.mem.mibMemFree, 0d);
            assertEquals(828740d, usage.mem.mibMemUsed, 0d);
        }

    }

}
