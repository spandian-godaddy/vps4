package com.godaddy.vps4.notifications.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.notifications.Notification;
import com.godaddy.vps4.notifications.NotificationFilter;
import com.godaddy.vps4.notifications.NotificationFilterType;
import com.godaddy.vps4.notifications.NotificationType;
import com.godaddy.vps4.notifications.NotificationExtendedDetails;
import com.godaddy.vps4.notifications.NotificationService;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.util.NotificationListSearchFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JdbcNotificationService implements NotificationService {
    private final DataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(JdbcNotificationService.class);
    private final String notificationTableName = "notification";

    @Inject
    public JdbcNotificationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void buildDateQuery(Instant validOnDate, Instant validUntilDate,
                                StringBuilder filtersQuery, ArrayList<Object> filterValues) {
        if(validOnDate != null) {
            filtersQuery.append(" and (n.valid_on <= ?) ");
            filterValues.add(LocalDateTime.ofInstant(validOnDate, ZoneOffset.UTC));
        }
        if(validUntilDate != null) {
            filtersQuery.append(" and (n.valid_until >= ? OR n.valid_until = 'infinity') ");
            filterValues.add(LocalDateTime.ofInstant(validUntilDate, ZoneOffset.UTC));
        }
    }

    private int buildFilterQuery(List<String> filters, StringBuilder filtersQuery, int filterTypeId, int andCount, ArrayList<Object> filterValues) {
        if(filters.size()>0) {
            if(andCount==0){
                filtersQuery.append(" and ( ( ");
                andCount++;
            }
            else
            {
                filtersQuery.append(" or ( ");
            }
            filtersQuery.append(" nf.filter_type_id = ?");
            filterValues.add(filterTypeId);

            filtersQuery.append(" and");
            filtersQuery.append(" nf.filter_value");
            String whereInClause = " && ARRAY[%s]::varchar[]";

            List<String> paramaterizedTokens = filters.stream().map(t -> "?").collect(Collectors.toList());
            whereInClause = String.format(whereInClause, String.join(",", paramaterizedTokens));
            filterValues.addAll(filters.stream().collect(Collectors.toList()));
            filtersQuery.append(whereInClause);
            filtersQuery.append(" ) ");
        }
        return andCount;
    }

    @Override
    public Notification getNotification(UUID notificationId) {
        return Sql.with(dataSource).exec("SELECT notification_id, nt.type, support_only," +
                        " dismissible, valid_on, valid_until, ned.start_time, ned.end_time, ned.translation_id" +
                        " FROM " + notificationTableName +
                        " as n JOIN notification_type AS nt USING(type_id)" +
                        " LEFT JOIN notification_extended_details AS ned USING(notification_id)" +
                        " WHERE n.notification_id = ?",
                Sql.nextOrNull(this::mapNotification), notificationId);
    }

    @Override
    public List<Notification> getNotifications(NotificationListSearchFilters searchFilters) {
        int andCount = 0;
        String primaryQuery = "SELECT DISTINCT n.notification_id, nt.type, support_only, dismissible," +
                " valid_on, valid_until, ned.start_time, ned.end_time, ned.translation_id" +
                " FROM " + notificationTableName + " as n" +
                " JOIN notification_type AS nt USING(type_id)" +
                " JOIN notification_filter AS nf USING(notification_id)" +
                " LEFT JOIN notification_extended_details AS ned USING(notification_id) WHERE 1=1 ";

        ArrayList<Object> filterValues = new ArrayList<>();
        StringBuilder filtersQuery = new StringBuilder();
        filtersQuery.append(primaryQuery);

        if(searchFilters.getShowActive()){
            filtersQuery.append(" and (n.valid_on<=now_utc()) AND (n.valid_until >= now_utc() OR n.valid_until = 'infinity') ");
        }
        buildDateQuery(searchFilters.getValidOn(), searchFilters.getValidUntil(), filtersQuery, filterValues);

        if(!searchFilters.getAdminView()){
            // if adminView is true then show all notifications, else show only customer's notifications
            filtersQuery.append(" and n.support_only = false ");
        }

        if(searchFilters.getTypeList().size()>0) {
            String whereInClause = "  and nt.type IN (%s)";
            List<String> paramaterizedTokens = searchFilters.getTypeList().stream().map(t -> "?").collect(Collectors.toList());
            whereInClause = String.format(whereInClause, String.join(",", paramaterizedTokens));
            filterValues.addAll(searchFilters.getTypeList().stream().map(s -> s.toString()).collect(Collectors.toList()));
            filtersQuery.append(whereInClause);
        }

        andCount = buildFilterQuery(searchFilters.getImageIds(), filtersQuery, NotificationFilterType.IMAGE_ID.getFilterTypeId(), andCount, filterValues);
        andCount = buildFilterQuery(searchFilters.getResellers(), filtersQuery, NotificationFilterType.RESELLER_ID.getFilterTypeId(), andCount, filterValues);
        andCount = buildFilterQuery(searchFilters.getHypervisor(), filtersQuery, NotificationFilterType.HYPERVISOR_HOSTNAME.getFilterTypeId(), andCount, filterValues);
        andCount = buildFilterQuery(searchFilters.getTiers(), filtersQuery, NotificationFilterType.TIER.getFilterTypeId(), andCount, filterValues);
        andCount = buildFilterQuery(searchFilters.getPlatformIds(), filtersQuery, NotificationFilterType.PLATFORM_ID.getFilterTypeId(), andCount, filterValues);
        andCount = buildFilterQuery(searchFilters.getVmIds(), filtersQuery, NotificationFilterType.VM_ID.getFilterTypeId(), andCount, filterValues);
        andCount = buildFilterQuery(searchFilters.getIsManagedAsList(), filtersQuery, NotificationFilterType.IS_MANAGED.getFilterTypeId(), andCount, filterValues);

        if(andCount > 0){
            filtersQuery.append(")");
        }
        filtersQuery.append(" ORDER BY valid_on DESC;");
        return Sql.with(dataSource).exec(filtersQuery.toString(), Sql.listOf(this::mapNotification), filterValues.toArray());
    }

    @Override
    public void deleteNotification(UUID notificationId){
        Sql.with(dataSource).exec("DELETE from notification_filter where notification_id = ? ",null, notificationId);
        Sql.with(dataSource).exec("DELETE from notification_extended_details where notification_id = ? ",null, notificationId);
        Sql.with(dataSource).exec("DELETE from notification where notification_id = ? ",null, notificationId);
    }

    @Override
    public List<NotificationFilterType> getFilters() {
        return Sql.with(dataSource).exec("SELECT * from notification_filter_type", Sql.listOf(this::mapFilterType));
    }

    @Override
    public Notification createNotification(UUID notificationId, NotificationType type, boolean supportOnly, boolean dismissible,
                                           NotificationExtendedDetails notificationExtendedDetails, List<NotificationFilter> filters,
                                           Instant validOn, Instant validUntil) {
        if (validOn == null && validUntil == null) {
            Sql.with(dataSource).exec("INSERT INTO " + notificationTableName + " (notification_id, type_id, support_only, dismissible) VALUES (?, ?, ?, ?);", null,
                    notificationId, type.getNotificationTypeId(), supportOnly, dismissible);
        }
        else {
            Sql.with(dataSource).exec("INSERT INTO " + notificationTableName + " (notification_id, type_id, support_only, dismissible, valid_on, valid_until) VALUES (?, ?, ?, ?, ?, ?);", null,
                    notificationId, type.getNotificationTypeId(), supportOnly, dismissible, LocalDateTime.ofInstant(validOn, ZoneOffset.UTC), LocalDateTime.ofInstant(validUntil, ZoneOffset.UTC));
        }
        if (notificationExtendedDetails != null) {
                Sql.with(dataSource).exec("INSERT INTO notification_extended_details (notification_id, start_time, end_time, translation_id) VALUES (?, ?, ?, ?);", null,
                        notificationId,
                        notificationExtendedDetails.start == null ? null : LocalDateTime.ofInstant(notificationExtendedDetails.start, ZoneOffset.UTC),
                        notificationExtendedDetails.end == null ? null : LocalDateTime.ofInstant(notificationExtendedDetails.end, ZoneOffset.UTC),
                        notificationExtendedDetails.translationId);

        }
        addFilterToNotification(notificationId, filters);
        return getNotification(notificationId);
    }

    @Override
    public Notification updateNotification(UUID notificationId, NotificationType type, boolean supportOnly, boolean dismissible,
                                           NotificationExtendedDetails notificationExtendedDetails, List<NotificationFilter> filters,
                                           Instant validOn, Instant validUntil) {
        Sql.with(dataSource).exec("UPDATE " + notificationTableName + " SET type_id = ?, support_only= ?, dismissible= ?, valid_on = ?, valid_until = ? WHERE notification_id = ?;", null,
                    type.getNotificationTypeId(), supportOnly, dismissible,
                     LocalDateTime.ofInstant(validOn == null ? Instant.now() : validOn, ZoneOffset.UTC),
                     validUntil == null ? LocalDateTime.MAX : LocalDateTime.ofInstant(validUntil, ZoneOffset.UTC),
                    notificationId);
        if(notificationExtendedDetails == null || (notificationExtendedDetails.start == null && notificationExtendedDetails.end == null &&
                notificationExtendedDetails.translationId == null)) {
            Sql.with(dataSource).exec("DELETE FROM notification_extended_details WHERE notification_id = ?;", null,
                    notificationId);
        }
        else {
            Sql.with(dataSource).exec("INSERT INTO notification_extended_details (notification_id, start_time, end_time, translation_id) VALUES (?, ?, ?, ?) " +
                            "ON CONFLICT (notification_id) DO UPDATE SET start_time = EXCLUDED.start_time," +
                            " end_time = EXCLUDED.end_time, translation_id = EXCLUDED.translation_id" +
                            " WHERE notification_extended_details.notification_id = EXCLUDED.notification_id;", null,
                    notificationId,
                    LocalDateTime.ofInstant(notificationExtendedDetails.start == null ? Instant.now() : notificationExtendedDetails.start, ZoneOffset.UTC),
                    notificationExtendedDetails.end == null ? LocalDateTime.MAX : LocalDateTime.ofInstant(notificationExtendedDetails.end, ZoneOffset.UTC),
                    notificationExtendedDetails.translationId);
        }
        Sql.with(dataSource).exec("DELETE FROM notification_filter WHERE notification_id = ?;", null,
                notificationId);
        addFilterToNotification(notificationId, filters);
        return getNotification(notificationId);
    }


    @Override
    public void addFilterToNotification(UUID notificationId, List<NotificationFilter> filters) {
        for (NotificationFilter filter : filters) {
            String filterValues  = String.join(",", filter.filterValue);
            Sql.with(dataSource).exec("INSERT INTO notification_filter (notification_id, filter_type_id, filter_value) VALUES (?, ?, string_to_array(?,',')) " +
                            "ON CONFLICT (notification_id, filter_type_id) DO UPDATE SET filter_value = EXCLUDED.filter_value WHERE " +
                            "notification_filter.notification_id = EXCLUDED.notification_id AND notification_filter.filter_type_id = EXCLUDED.filter_type_id;", null,
                    notificationId, filter.filterType.getFilterTypeId(), filterValues);
        }
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        NotificationType type = NotificationType.valueOf(rs.getString("type"));
        Notification notification = new Notification();

        notification.type = type;
        notification.notificationId = UUID.fromString(rs.getString("notification_id"));
        notification.dismissible = rs.getBoolean("dismissible");

        notification.supportOnly = rs.getBoolean("support_only");
        notification.validOn = rs.getObject("valid_on") == null ?  null :
                rs.getTimestamp("valid_on", TimestampUtils.utcCalendar).toInstant();
        notification.validUntil = rs.getObject("valid_until") == null ?  null :
                rs.getTimestamp("valid_until", TimestampUtils.utcCalendar).toInstant();

        NotificationExtendedDetails notificationExtendedDetails = new NotificationExtendedDetails();
        notificationExtendedDetails.start = rs.getObject("start_time") == null ?  null :
                 rs.getTimestamp("start_time", TimestampUtils.utcCalendar).toInstant();
        notificationExtendedDetails.end = rs.getObject("end_time") == null ?  null :
                rs.getTimestamp("end_time", TimestampUtils.utcCalendar).toInstant();
        notificationExtendedDetails.translationId = rs.getString("translation_id");
        notification.notificationExtendedDetails = notificationExtendedDetails;
        notification.filters = getNotificationFilters(notification.notificationId);
        return notification;

    }

    protected List<NotificationFilter> getNotificationFilters(UUID notificationId){
        return Sql.with(dataSource).exec("SELECT filter_value, filter_type  FROM notification_filter JOIN notification_filter_type USING(filter_type_id) WHERE notification_id = ?;",
                Sql.listOf(this::mapFilter), notificationId);
    }

    private NotificationFilter mapFilter(ResultSet rs) throws SQLException {
        NotificationFilterType type = NotificationFilterType.valueOf(rs.getString("filter_type"));
        NotificationFilter notificationFilter = new NotificationFilter();
        String[] arrays = (String[])rs.getArray("filter_value").getArray();
        notificationFilter.filterType = type;
        notificationFilter.filterValue = Arrays.asList(arrays);
        return notificationFilter;
    }

    private NotificationFilterType mapFilterType(ResultSet rs) throws SQLException {
        return NotificationFilterType.valueOf(rs.getString("filter_type"));
    }
}
