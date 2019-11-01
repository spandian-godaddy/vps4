package com.godaddy.vps4.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

public class ActionListUtils {

    private final String tableName;
    private final String resourceIdFieldName;
    private final DataSource dataSource;

    public ActionListUtils(String tableName, String resourceIdFieldName, DataSource dataSource) {
        this.tableName = tableName;
        this.resourceIdFieldName = resourceIdFieldName;
        this.dataSource = dataSource;
    }

    public ResultSubset<Action> getActions(ActionListFilters actionFilters) {
        String initialQuery = "SELECT *, count(*) over() as total_rows FROM " + tableName
                + " JOIN action_status on " + tableName + ".status_id = action_status.status_id"
                + " JOIN action_type on " + tableName + ".action_type_id = action_type.type_id WHERE 1=1 ";

        Map<String, Object> filterParams = new HashMap<>();
        UUID resourceId = actionFilters.getResourceId();
        if (resourceId != null) {
            filterParams.put(resourceIdFieldName, resourceId);
        }

        ArrayList<Object> filterValues = new ArrayList<>();
        StringBuilder actionsQuery = new StringBuilder();
        actionsQuery.append(initialQuery);
        for (Map.Entry<String, Object> pair : filterParams.entrySet()) {
            actionsQuery.append(" and ");
            actionsQuery.append(pair.getKey());
            actionsQuery.append("=?");
            filterValues.add(pair.getValue());
        }

        addStatusFilter(actionFilters.getStatusList(), filterValues, actionsQuery);
        addActionTypeFilter(actionFilters.getTypeList(), filterValues, actionsQuery);
        buildDateQuery(actionFilters.getStart(), actionFilters.getEnd(), filterValues, actionsQuery);

        actionsQuery.append(" ORDER BY created DESC ");
        long limit = actionFilters.getLimit();
        if (limit >= 0) {
            actionsQuery.append("LIMIT ? ");
            filterValues.add(limit);
        }
        long offset = actionFilters.getOffset();
        if (offset > 0) {
            actionsQuery.append("OFFSET ?;");
            filterValues.add(offset);
        }

        return Sql.with(dataSource).exec(actionsQuery.toString(), Sql.nextOrNull(this::mapActionWithTotal),
                filterValues.toArray());
    }

    private void addActionTypeFilter(List<ActionType> typeList, ArrayList<Object> filterValues,
            StringBuilder actionsQuery) {
        if (typeList.isEmpty())
            return;

        String whereInClause = " AND action_type.type IN (%s)";
        List<String> paramaterizedTokens = typeList.stream().map(t -> "?").collect(Collectors.toList());
        whereInClause = String.format(whereInClause, String.join(",", paramaterizedTokens));

        filterValues.addAll(typeList.stream().map(t -> t.toString()).collect(Collectors.toList()));
        actionsQuery.append(whereInClause);
    }

    private void addStatusFilter(List<ActionStatus> statusList, ArrayList<Object> filterValues,
            StringBuilder actionsQuery) {
        if (statusList.isEmpty())
            return;

        String whereInClause = " AND action_status.status IN (%s)";
        List<String> paramaterizedTokens = statusList.stream().map(t -> "?").collect(Collectors.toList());
        whereInClause = String.format(whereInClause, String.join(",", paramaterizedTokens));

        filterValues.addAll(statusList.stream().map(s -> s.toString()).collect(Collectors.toList()));
        actionsQuery.append(whereInClause);
    }

    private void buildDateQuery(Instant beginDate, Instant endDate, ArrayList<Object> filterValues,
            StringBuilder actionsQuery) {
        if (beginDate != null) {
            actionsQuery.append(" and created >= ?");
            filterValues.add(LocalDateTime.ofInstant(beginDate, ZoneOffset.UTC));
        }
        if (endDate != null) {
            actionsQuery.append(" and created <= ?");
            filterValues.add(LocalDateTime.ofInstant(endDate, ZoneOffset.UTC));
        }
    }

    private ResultSubset<Action> mapActionWithTotal(ResultSet rs) throws SQLException {
        long totalRows = rs.getLong("total_rows");
        List<Action> actions = new ArrayList<>();
        actions.add(mapAction(rs));
        while (rs.next()) {
            actions.add(mapAction(rs));
        }
        return new ResultSubset<Action>(actions, totalRows);
    }

    private Action mapAction(ResultSet rs) throws SQLException {
        ActionStatus status = ActionStatus.valueOf(rs.getString("status"));
        ActionType type = ActionType.valueOf(rs.getString("type"));

        String resourceIdStr = rs.getString(resourceIdFieldName);
        UUID resourceId = null;
        if (resourceIdStr != null) {
            resourceId = UUID.fromString(resourceIdStr);
        }

        String commandIdStr = rs.getString("command_id");
        UUID commandId = null;
        if (commandIdStr != null) {
            commandId = UUID.fromString(commandIdStr);
        }

        Timestamp completedTs = rs.getTimestamp("completed", TimestampUtils.utcCalendar);
        Instant completed = null;
        if (completedTs != null) {
            completed = completedTs.toInstant();
        }

        return new Action(rs.getLong("id"), resourceId, type, rs.getString("request"), rs.getString("state"),
                rs.getString("response"), status, rs.getTimestamp("created", TimestampUtils.utcCalendar).toInstant(),
                completed, rs.getString("note"), commandId, rs.getString("initiated_by"));
    }
}
