package com.godaddy.vps4.web.notification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.notifications.Notification;
import com.godaddy.vps4.notifications.NotificationService;
import com.godaddy.vps4.notifications.NotificationType;
import com.godaddy.vps4.notifications.NotificationFilter;
import com.godaddy.vps4.notifications.NotificationFilterType;
import com.godaddy.vps4.notifications.NotificationExtendedDetails;
import com.godaddy.vps4.util.NotificationListSearchFilters;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnDateInstant;
import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;

@Vps4Api
@Api(tags = {"notifications"})

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN})
public class NotificationsResource {
    private static final Logger logger = LoggerFactory.getLogger(NotificationsResource.class);

    private final NotificationService notificationService;
    @Inject
    public NotificationsResource(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @DELETE
    @Path("/{notificationId}/")
    public void deleteNotifications(@PathParam("notificationId") UUID notifId) {
        notificationService.deleteNotification(notifId);
    }

    @GET
    @Path("/{notificationId}")
    public Notification getNotification(@PathParam("notificationId") UUID notificationId) {
        return notificationService.getNotification(notificationId);
    }

    @GET
    @Path("/")
    public List<Notification> getNotificationsBasedOnFilters(
            @ApiParam(value = "A list of image ids to filter on", required = false) @QueryParam("imageId") List<String> imageId,
            @ApiParam(value = "A list of reseller ids to filter on", required = false) @QueryParam("resellerId") List<String> resellerId,
            @ApiParam(value = "A list of notification types to filter on", required = false) @QueryParam("type") List<String> type,
            @ApiParam(value = "valid on date of notification in UTC, Example: 2007-12-03T10:15:30.00Z", required = false) @QueryParam("beginDate") String validOn,
            @ApiParam(value = "valid until date  of notification in UTC, Example: 2007-12-03T10:15:30.00Z", required = false) @QueryParam("endDate") String validUntil,
            @ApiParam(value = "A list of hypervisors to filter on", required = false) @QueryParam("hypervisor") List<String> hypervisor,
            @ApiParam(value = "A list of tier to filter on", required = false) @QueryParam("tier") List<String> tier,
            @ApiParam(value = "A list of platforms to filter on", required = false) @QueryParam("platformId") List<String> platformId,
            @ApiParam(value = "A list of vmIds to filter on", required = false) @QueryParam("vmId") List<String> vmId,
            @DefaultValue("true") @QueryParam("showActive") boolean showActive,
            @ApiParam(value = "Whether to show all notifications including support only ones", required = false)
            @DefaultValue("false") @QueryParam("adminView") boolean adminView)
    {
        List<NotificationType> enumTypeList = new ArrayList<>();
        if(type != null) {
            enumTypeList = type.stream()
                    .map(t -> validateAndReturnEnumValue(NotificationType.class, t))
                    .collect(Collectors.toList());
        }
        Instant startTime = validateAndReturnDateInstant(validOn);
        Instant endTime = validateAndReturnDateInstant(validUntil);
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byResellerId(resellerId);
        searchFilters.byImageId(imageId);
        searchFilters.byHypervisors(hypervisor);
        searchFilters.byType(enumTypeList);
        searchFilters.byTier(tier);
        searchFilters.byPlatform(platformId);
        searchFilters.byDateRange(startTime, endTime);
        searchFilters.byActive(showActive);
        searchFilters.byVmId(vmId);
        searchFilters.byAdminView(adminView);
        return notificationService.getNotifications(searchFilters);
    }

    @POST
    @Path("/")
    public Notification createNewNotification(NotificationRequest request) {
        UUID notificationId = UUID.randomUUID();
        return notificationService.createNotification(notificationId, request.type,
                request.supportOnly, request.dismissible, request.notificationExtendedDetails, request.filters,
                request.validOn, request.validUntil);
    }

    @POST
    @Path("/filter")
    public UUID addFilterToNotification(NotificationFilterRequest request) {
        UUID notificationId = UUID.randomUUID();
        notificationService.addFilterToNotification(request.notificationId, request.filters);
        return notificationId;
    }

    @GET
    @Path("/filter")
    public List<NotificationFilterType> getFilters() {
        return notificationService.getFilters();
    }

    @PUT
    @Path("/{notificationId}")
    public Notification putNotification(@PathParam("notificationId") UUID notificationId, NotificationRequest request) {
         if (notificationService.getNotification(notificationId) == null)
         {
             throw new NotFoundException("Unknown notification ID: " + notificationId);
         }
         return notificationService.updateNotification(notificationId, request.type,
                request.supportOnly, request.dismissible, request.notificationExtendedDetails, request.filters,
                request.validOn, request.validUntil);
    }

    public static class NotificationRequest {
        public NotificationType type;
        public boolean supportOnly;
        public boolean dismissible;
        public NotificationExtendedDetails notificationExtendedDetails;
        public Instant validOn;
        public Instant validUntil;
        public List<NotificationFilter> filters;
    }

    public static class NotificationFilterRequest {
        public List<NotificationFilter> filters;
        public UUID notificationId;
    }
}
