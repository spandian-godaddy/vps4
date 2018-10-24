package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.godaddy.vps4.jdbc.ResultSubset;

public interface ActionService {

    long createAction(UUID resourceId, ActionType actionType, String request, String initiatedBy);

    Action getAction(long actionId);

    Action getAction(UUID resourceId, long actionId);

    default List<Action> getActions(UUID resourceId) {
        throw new UnsupportedOperationException("Not implemented, yet");
    }

    void tagWithCommand(long actionId, UUID commandId);

    void markActionInProgress(long actionId);

    void updateActionState(long actionId, String state);

    void completeAction(long actionId, String response, String notes);

    void failAction(long actionId, String response, String notes);

    void cancelAction(long actionId, String response, String notes);

    List<Action> getIncompleteActions(UUID resourceId);

    ResultSubset<Action> getActionList(ActionListFilters filters);

    static class ActionListFilters {
        private UUID vmId;
        private List<ActionStatus> statusList = new ArrayList<>();
        private List<ActionType> typeList = new ArrayList<>();
        private Instant start;
        private Instant end;
        private long limit = -1; // Default no limit
        private long offset;

        public void byVmId(UUID vmId) {
            this.vmId = vmId;
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

        public UUID getVmId() {
            return this.vmId;
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
}
