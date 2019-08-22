package com.godaddy.vps4.hfs;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import io.swagger.annotations.ApiModelProperty;

public class HfsVmTrackingRecord {
    public long hfsVmId;
    public UUID vmId;
    public UUID orionGuid;
    public String sgid;
    public long createActionId;
    public long cancelActionId;
    public long destroyActionId;

    @ApiModelProperty(
            value = "The timestamp of when this vm was requested. This is an ISO 8601 formatted date string",
            example = "2019-01-24T16:52:55Z",
            dataType = "java.lang.String")
    public Instant requested;

    @ApiModelProperty(
            value = "The timestamp of when the requested vm was created. This is an ISO 8601 formatted date string",
            example = "2019-01-24T16:52:55Z",
            dataType = "java.lang.String")
    public Instant created;

    @ApiModelProperty(
            value = "The timestamp of when the vm was canceled (i.e. zombie'd). This is an ISO 8601 formatted date " +
                    "string",
            example = "2019-01-24T16:52:55Z",
            dataType = "java.lang.String")
    public Instant canceled;

    @ApiModelProperty(
            value = "The timestamp of when the vm was destroyed. This is an ISO 8601 formatted date string",
            example = "2019-01-24T16:52:55Z",
            dataType = "java.lang.String")
    public Instant destroyed;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
