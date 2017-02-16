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

    public VmUsage parse(JSONObject json) {

        VmUsage usage = new VmUsage();

        usage.disk = parseDisk((JSONObject)json.get("hdd"));

        usage.cpu = parseCpu((JSONObject)json.get("cpu"));

        usage.mem = parseMemory((JSONObject)json.get("mem"));

        usage.io = parseIo((JSONObject)json.get("io"));

        return usage;
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

    protected IoUsage parseIo(JSONObject json) {

        if (json == null) {
            return null;
        }

        Row row = readMostRecentRow(json);
        if (row == null) {
            return null;
        }

        IoUsage usage = new IoUsage();
        usage.timestamp = row.timestamp;

        if (row.containsHeader("tps")) {
            // linux
            usage.readTps = Double.parseDouble(row.getValue("rtps"));
            usage.writeTps = Double.parseDouble(row.getValue("wtps"));
            usage.totalTps = Double.parseDouble(row.getValue("tps"));

        } else if (row.containsHeader("Disk Transfers/sec")) {
            // windows
            usage.readTps = Double.parseDouble(row.getValue("Disk Reads/sec"));
            usage.writeTps = Double.parseDouble(row.getValue("Disk Writes/sec"));
            usage.totalTps = Double.parseDouble(row.getValue("Disk Transfers/sec"));

        } else {
            return null;
        }

        return usage;
    }

    protected DiskUsage parseDisk(JSONObject json) {

        if (json == null) {
            return null;
        }

        Row row = readMostRecentRow(json);
        if (row == null) {
            return null;
        }

        DiskUsage usage = new DiskUsage();
        usage.timestamp = row.timestamp;

        if (row.containsHeader("Used")) {
            // linux
            usage.mibUsed = Long.parseLong(row.getValue("Used"));
            usage.mibAvail = Long.parseLong(row.getValue("Avail"));

        } else if (row.containsHeader("% Free Space")) {
            // windows
            long freeMb = Long.parseLong(row.getValue("Free Megabytes"));
            double pctFreeSpace = Double.parseDouble(row.getValue("% Free Space")) / 100d;
            long availMb = (long)(((double)freeMb) / pctFreeSpace);

            usage.mibUsed = freeMb;
            usage.mibAvail = availMb;

        } else {
            return null;
        }

        return usage;
    }

    protected CpuUsage parseCpu(JSONObject json) {

        if (json == null) {
            return null;
        }

        Row row = readMostRecentRow(json);
        if (row == null) {
            return null;
        }

        CpuUsage usage = new CpuUsage();
        usage.timestamp = row.timestamp;

        if (row.containsHeader("%user")) {
            // linux
            usage.userPercent = Double.parseDouble(row.getValue("%user"));
            // TODO add '%nice' value to 'userPercent' here?
            usage.systemPercent = Double.parseDouble(row.getValue("%system"));

        } else if (row.containsHeader("% User Time")) {
            // windows
            usage.userPercent = Double.parseDouble(row.getValue("% User Time"));
            usage.systemPercent = Double.parseDouble(row.getValue("% Processor Time"));

        } else {
            return null;
        }

        return usage;
    }

    protected MemUsage parseMemory(JSONObject json) {
        if (json == null) {
            return null;
        }

        Row row = readMostRecentRow(json);
        if (row == null) {
            return null;
        }

        MemUsage usage = new MemUsage();
        usage.timestamp = row.timestamp;

        if (row.containsHeader("kbcommit")) {
            // linux
            usage.mibMemFree = Long.parseLong(row.getValue("kbmemfree")) / 1024L;
            usage.mibMemUsed = Long.parseLong(row.getValue("kbmemused")) / 1024L;

        } else if (row.containsHeader("Available KBytes")) {
            // windows
            long mibMemAvail = Long.parseLong(row.getValue("Available KBytes")) / 1024L;
            long mibMemCommitted = Long.parseLong(row.getValue("Committed Bytes")) / 1024L;
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
