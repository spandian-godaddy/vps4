package com.godaddy.hfs.vm;

import java.util.Objects;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "status": "DEACTIVATED",
  "usage": {
    "unit": null,
    "value": null
  },
  "quota": {
    "unit": null,
    "value": null
  },
  "vm_id": 24023,
  "id": 1,
  "ftpBackupName": null,
  "readOnlyDate": null,
  "type": null
}
*/

public class BackupStorage {
    public Status status;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public DiskSpace usage;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public DiskSpace quota;

    @JsonGetter
    public long diskUsed() {
        return Objects.equals(usage.unit, "%")
            ? getValueMib(quota) * usage.value / 100
            : getValueMib(usage);
    };

    @JsonGetter
    public long diskTotal() {
        return getValueMib(quota);
    };

    public enum Status {
        UNKNOWN,
        ACTIVATED,
        ACTIVATING,
        DEACTIVATED,
        DEACTIVATING
    }

    private static class DiskSpace {
        public String unit;
        public long value;

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    private long getValueMib(DiskSpace space) {
        if (space.unit != null) {
            switch (space.unit) {
                case "MB":
                    return space.value;
                case "GB":
                    return space.value * 1024;
                case "TB":
                    return space.value * 1024 * 1024;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
