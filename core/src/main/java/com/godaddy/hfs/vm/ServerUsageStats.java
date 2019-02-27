package com.godaddy.hfs.vm;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * This class is a JSON representation of the elements returned in the response as shown below:
 * {
 * "utilizationId": 31,
 * "vmId": 5563,
 * "requested": "2019-02-01T16:50:45.881000-07:00",
 * "workflowId": "317aa49a267c11e98aa7fa163ead5011",
 * "collected": "2019-02-01T16:50:56.702531-07:00",
 * "diskTotal": 92148,
 * "diskUsed": 36339,
 * "memoryTotal": 5805,
 * "memoryUsed": 5548,
 * "cpuUsed": 0.33
 * }
 */

public class ServerUsageStats {

    private long utilizationId;
    private long vmId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ZonedDateTime requested;
    private String workflowId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ZonedDateTime collected;
    private long diskTotal;
    private long diskUsed;
    private long memoryTotal;
    private long memoryUsed;
    private double cpuUsed;

    // cannot have a request pending for more than 5 minutes.
    private final int PENDING_REFRESH_TIMEOUT = 5;

    public ServerUsageStats() {
    }

    /**
     * All fields constructor makes creating object easy for testing.
     */
    public ServerUsageStats(long utilizationId,
                            long vmId,
                            ZonedDateTime requested,
                            String workflowId,
                            ZonedDateTime collected,
                            long diskTotal,
                            long diskUsed,
                            long memoryTotal,
                            long memoryUsed,
                            double cpuUsed) {

        this.utilizationId = utilizationId;
        this.vmId = vmId;
        this.requested = requested;
        this.workflowId = workflowId;
        this.collected = collected;
        this.diskTotal = diskTotal;
        this.diskUsed = diskUsed;
        this.memoryTotal = memoryTotal;
        this.memoryUsed = memoryUsed;
        this.cpuUsed = cpuUsed;
    }

    public long getUtilizationId() {
        return utilizationId;
    }

    public void setUtilizationId(long utilizationId) {
        this.utilizationId = utilizationId;
    }

    public long getVmId() {
        return vmId;
    }

    public void setVmId(long vmId) {
        this.vmId = vmId;
    }


    public ZonedDateTime getRequested() {
        return requested;
    }

    public void setRequested(ZonedDateTime requested) {
        this.requested = requested;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public ZonedDateTime getCollected() {
        return collected;
    }

    public void setCollected(ZonedDateTime collected) {
        this.collected = collected;
    }

    public long getDiskTotal() {
        return diskTotal;
    }

    public void setDiskTotal(long diskTotal) {
        this.diskTotal = diskTotal;
    }

    public long getDiskUsed() {
        return diskUsed;
    }

    public void setDiskUsed(long diskUsed) {
        this.diskUsed = diskUsed;
    }

    public long getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(long memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public double getCpuUsed() {
        return cpuUsed;
    }

    public void setCpuUsed(double cpuUsed) {
        this.cpuUsed = cpuUsed;
    }

    public boolean areStale() {
        return getRequested().toInstant().isBefore(Instant.now().minus(PENDING_REFRESH_TIMEOUT, ChronoUnit.MINUTES));
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
