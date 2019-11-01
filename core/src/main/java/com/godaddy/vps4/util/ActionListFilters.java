package com.godaddy.vps4.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ActionListFilters {
    private UUID resourceId;
    private List<ActionStatus> statusList = new ArrayList<>();
    private List<ActionType> typeList = new ArrayList<>();
    private Instant start;
    private Instant end;
    private long limit = -1; // Default no limit
    private long offset;

    public void byResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public void byStatus(ActionStatus... statuses) {
        statusList = Arrays.asList(statuses);
    }

    public void byStatus(Collection<ActionStatus> statuses) {
        this.statusList = new ArrayList<>(statuses);
    }

    public void byType(ActionType... types) {
        typeList = Arrays.asList(types);
    }

    public void byType(Collection<ActionType> types) {
        this.typeList = new ArrayList<>(types);
    }

    public void byDateRange(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public UUID getResourceId() {
        return this.resourceId;
    }

    public List<ActionStatus> getStatusList() {
        return statusList;
    }

    public List<ActionType> getTypeList() {
        return typeList;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }

    public long getLimit() {
        return limit;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
