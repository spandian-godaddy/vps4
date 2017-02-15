package com.godaddy.vps4.sysadmin;

import java.util.List;

import org.json.simple.JSONObject;

import com.godaddy.vps4.sysadmin.VmUsage.CpuUsage;
import com.godaddy.vps4.sysadmin.VmUsage.DiskUsage;
import com.godaddy.vps4.sysadmin.VmUsage.IoUsage;
import com.godaddy.vps4.sysadmin.VmUsage.MemUsage;

public class VmUsageParser {

    List<String> headers;

    List<List<String>> data;

    public VmUsage parse(JSONObject json) {

        VmUsage usage = new VmUsage();

        usage.disk = parseDisk((JSONObject)json.get("hdd"));

        usage.cpu = parseCpu((JSONObject)json.get("cpu"));

        usage.mem = parseMemory((JSONObject)json.get("mem"));

        usage.io = parseIo((JSONObject)json.get("io"));

        return usage;
    }

    @SuppressWarnings("unchecked")
    protected void readStructure(JSONObject json) {
        headers = (List<String>)json.get("headers"); // aray of string column names
        data = (List<List<String>>)json.get("data"); // array of arrays of string column values
        if (headers == null || data == null) {
            throw new IllegalArgumentException("Unexpected data format ('headers'/'data')");
        }
    }

    protected IoUsage parseIo(JSONObject json) {

        if (json == null) {
            return null;
        }

        IoUsage usage = new IoUsage();

        readStructure(json);

        if (data.size() < 1) {
            return null;
        }

        // TODO look for the most recent data, based on 'Timestamp' header
        //  with format:  yyyy-MM-dd HH:mm:ss UTC
        List<String> row = data.get(0);

        if (headers.contains("tps")) {
            // linux
            usage.readTps = Double.parseDouble(getValue("rtps", row));
            usage.writeTps = Double.parseDouble(getValue("wtps", row));
            usage.totalTps = Double.parseDouble(getValue("tps", row));

        } else if (headers.contains("Disk Transfers/sec")) {
            // windows
            usage.readTps = Double.parseDouble(getValue("Disk Reads/sec", row));
            usage.writeTps = Double.parseDouble(getValue("Disk Writes/sec", row));
            usage.totalTps = Double.parseDouble(getValue("Disk Transfers/sec", row));

        } else {
            return null;
        }

        return usage;
    }

    protected DiskUsage parseDisk(JSONObject json) {

        if (json == null) {
            return null;
        }

        DiskUsage usage = new DiskUsage();

        readStructure(json);

        if (data.size() < 1) {
            return null;
        }

        // TODO look for the most recent data, based on 'Timestamp' header
        //  with format:  yyyy-MM-dd HH:mm:ss UTC
        List<String> row = data.get(0);

        if (headers.contains("Used")) {
            // linux
            usage.mibUsed = Long.parseLong(getValue("Used", row));
            usage.mibAvail = Long.parseLong(getValue("Avail", row));

        } else if (headers.contains("% Free Space")) {
            // windows
            long freeMb = Long.parseLong(getValue("Free Megabytes", row));
            double pctFreeSpace = Double.parseDouble(getValue("% Free Space", row)) / 100d;
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

        CpuUsage usage = new CpuUsage();

        readStructure(json);

        if (data.size() < 1) {
            return null;
        }

        List<String> row = data.get(0);

        if (headers.contains("%user")) {
            // linux
            usage.userPercent = Double.parseDouble(getValue("%user", row));
            // TODO add '%nice' value to 'userPercent' here?
            usage.systemPercent = Double.parseDouble(getValue("%system", row));

        } else if (headers.contains("% User Time")) {
            // windows
            usage.userPercent = Double.parseDouble(getValue("% User Time", row));
            usage.systemPercent = Double.parseDouble(getValue("% Processor Time", row));

        } else {
            return null;
        }

        return usage;
    }

    protected MemUsage parseMemory(JSONObject json) {
        if (json == null) {
            return null;
        }

        MemUsage usage = new MemUsage();

        readStructure(json);

        if (data.size() < 1) {
            return null;
        }

        List<String> row = data.get(0);

        if (headers.contains("kbcommit")) {
            // linux
            usage.mibMemFree = Long.parseLong(getValue("kbmemfree", row)) / 1024L;
            usage.mibMemUsed = Long.parseLong(getValue("kbmemused", row)) / 1024L;

        } else if (headers.contains("Available KBytes")) {
            // windows
            long mibMemAvail = Long.parseLong(getValue("Available KBytes", row)) / 1024L;
            long mibMemCommitted = Long.parseLong(getValue("Committed Bytes", row)) / 1024L;
            usage.mibMemFree = mibMemAvail;
            usage.mibMemUsed = mibMemCommitted;

        } else {
            return null;
        }

        return usage;
    }

    protected String getValue(String columnName, List<String> row) {
        int index = headers.indexOf(columnName);
        if (index == -1) {
            throw new IllegalArgumentException("Missing '" + columnName + "' header");
        }
        if (index >= row.size()) {
            throw new IllegalArgumentException("Column '" + columnName + "' missing from data");
        }
        return row.get(index);
    }

}
