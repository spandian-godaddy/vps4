package com.godaddy.vps4.sysadmin;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.sysadmin.VmUsage.CpuUsage;
import com.godaddy.vps4.sysadmin.VmUsage.DiskUsage;
import com.godaddy.vps4.sysadmin.VmUsage.IoUsage;
import com.godaddy.vps4.sysadmin.VmUsage.MemUsage;

public class VmUsageParser {

    static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private static final Logger logger = LoggerFactory.getLogger(VmUsageParser.class);

    private JSONObject json;

    public VmUsageParser(JSONObject json) {
        this.json = json;
    }

    public VmUsage parse() {
        VmUsage usage = new VmUsage();
        usage.disk = parseDisk();
        usage.cpu = parseCpu();
        usage.mem = parseMemory();
        usage.io = parseIo();
        return usage;
    }

    private long parseLong(String val, String valType) {
        try {
            return Long.parseLong(val);
        }
        catch (NumberFormatException e) {
            logger.warn("Unable to parse value for type: {}", valType);
            return 0L; // If we are unable to parse then return a default value
        }
    }

    private double parseDouble(String val, String valType) {
        try {
            return Double.parseDouble(val);
        }
        catch (NumberFormatException e) {
            logger.warn("Unable to parse value for type: {}", valType);
            return 0.0d; // If we are unable to parse then return a default value
        }
    }

    @SuppressWarnings("unchecked")
    protected Row readMostRecentRow(JSONObject json) {
        List<String> headers = (List<String>)json.get("headers"); // aray of string column names
        List<List<String>> data = (List<List<String>>)json.get("data"); // array of arrays of string column values
        if (headers == null || data == null) {
            throw new IllegalArgumentException("Unexpected data format ('headers'/'data')");
        }

        int timestampIndex = headers.indexOf("Timestamp");
        if (timestampIndex == -1) {
            return null;
        }

        Row row = new Row();
        row.headers = headers;

        Instant mostRecent = null;

        for (int i=0; i<data.size(); i++) {
            List<String> values = data.get(i);

            String sTimestamp = values.get(timestampIndex);
            try {
                Instant timestamp = FORMAT.parse(sTimestamp, Instant::from);

                if (mostRecent == null || timestamp.isAfter(mostRecent)) {
                    mostRecent = timestamp;
                    row.values = values;
                    row.timestamp = timestamp;
                }

            } catch (DateTimeParseException e) {
                logger.warn("Unable to parse timestamp: {}", sTimestamp);
            }
        }

        if (row.values == null) {
            return null;
        }

        return row;
    }

    protected IoUsage parseIo() {

        if (json == null) {
            return null;
        }

        JSONObject ioJson = (JSONObject)json.get("io");
        if (ioJson == null) {
            return null;
        }

        Row row = readMostRecentRow(ioJson);
        if (row == null) {
            return null;
        }

        IoUsage usage = new IoUsage();
        usage.timestamp = row.timestamp;

        if (row.containsHeader("tps")) {
            // linux
            usage.readTps = parseDouble(row.getValue("rtps"), "rtps");
            usage.writeTps = parseDouble(row.getValue("wtps"), "wtps");
            usage.totalTps = parseDouble(row.getValue("tps"), "tps");

        } else if (row.containsHeader("Disk Transfers/sec")) {
            // windows
            usage.readTps = parseDouble(row.getValue("Disk Reads/sec"), "Disk Reads/sec");
            usage.writeTps = parseDouble(row.getValue("Disk Writes/sec"), "Disk Writes/sec");
            usage.totalTps = parseDouble(row.getValue("Disk Transfers/sec"), "Disk Transfers/sec");

        } else {
            return null;
        }

        return usage;
    }

    protected DiskUsage parseDisk() {

        if (json == null) {
            return null;
        }

        JSONObject diskJson = (JSONObject)json.get("hdd");
        if (diskJson == null) {
            return null;
        }

        Row row = readMostRecentRow(diskJson);
        if (row == null) {
            return null;
        }

        DiskUsage usage = new DiskUsage();
        usage.timestamp = row.timestamp;

        if (row.containsHeader("Used")) {
            // linux
            usage.mibUsed = parseLong(row.getValue("Used"), "Used");
            usage.mibAvail = parseLong(row.getValue("Avail"), "Avail");

        } else if (row.containsHeader("% Free Space")) {
            // windows
            long freeMb = parseLong(row.getValue("Free Megabytes"), "Free Megabytes");
            double pctFreeSpace = parseDouble(row.getValue("% Free Space"), "% Free Space") / 100d;
            long totalMbs = (long)((freeMb) / pctFreeSpace);

            usage.mibAvail = freeMb;
            usage.mibUsed = totalMbs - freeMb;

        } else {
            return null;
        }

        return usage;
    }

    protected CpuUsage parseCpu() {

        if (json == null) {
            return null;
        }

        JSONObject cpuJson = (JSONObject)json.get("cpu");
        if (cpuJson == null) {
            return null;
        }

        Row row = readMostRecentRow(cpuJson);
        if (row == null) {
            return null;
        }

        CpuUsage usage = new CpuUsage();
        usage.timestamp = row.timestamp;

        if (row.containsHeader("%user")) {
            // linux
            usage.userPercent = parseDouble(row.getValue("%user"), "%user");
            // TODO add '%nice' value to 'userPercent' here?
            usage.systemPercent = parseDouble(row.getValue("%system"), "%system");

        } else if (row.containsHeader("% User Time")) {
            // windows
            usage.userPercent = parseDouble(row.getValue("% User Time"), "% User Time");
            usage.systemPercent = parseDouble(row.getValue("% Processor Time"), "% Processor Time");

        } else {
            return null;
        }

        return usage;
    }

    protected MemUsage parseMemory() {
        if (json == null) {
            return null;
        }

        JSONObject memJson = (JSONObject)json.get("mem");
        if (memJson == null) {
            return null;
        }

        Row row = readMostRecentRow(memJson);
        if (row == null) {
            return null;
        }

        MemUsage usage = new MemUsage();
        usage.timestamp = row.timestamp;

        if (row.containsHeader("kbcommit")) {
            // linux
            long kbMemFree = parseLong(row.getValue("kbmemfree"), "kbmemfree");
            long kbMemUsed = parseLong(row.getValue("kbmemused"), "kbmemused");
            long kbCached = parseLong(row.getValue("kbcached"), "kbcached");
            long kbBuffers = parseLong(row.getValue("kbbuffers"), "kbmbuffers");
            usage.mibMemFree = (kbMemFree + kbCached + kbBuffers) / 1024L;
            usage.mibMemUsed = (kbMemUsed - kbCached - kbBuffers) / 1024L;

        } else if (row.containsHeader("Available KBytes")) {
            // windows
            long mibMemAvail = parseLong(row.getValue("Available KBytes"), "Available KBytes") / 1024L;
            long mibMemCommitted = parseLong(row.getValue("Committed Bytes"), "Committed Bytes") / 1024L / 1024L;
            usage.mibMemFree = mibMemAvail;
            usage.mibMemUsed = mibMemCommitted;

        } else {
            return null;
        }

        return usage;
    }



    static class Row {
        Instant timestamp;
        List<String> headers;
        List<String> values;

        boolean containsHeader(String headerName) {
            return headers.contains(headerName);
        }

        String getValue(String columnName) {
            int index = headers.indexOf(columnName);
            if (index == -1) {
                throw new IllegalArgumentException("Missing '" + columnName + "' header");
            }
            if (index >= values.size()) {
                throw new IllegalArgumentException("Column '" + columnName + "' missing from data");
            }
            return values.get(index);
        }
    }

}
