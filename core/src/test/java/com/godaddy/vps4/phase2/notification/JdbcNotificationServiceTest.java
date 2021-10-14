package com.godaddy.vps4.phase2.notification;

import com.godaddy.vps4.jdbc.DatabaseModule;

import com.godaddy.vps4.notifications.Notification;
import com.godaddy.vps4.notifications.NotificationFilter;
import com.godaddy.vps4.notifications.NotificationType;
import com.godaddy.vps4.notifications.NotificationExtendedDetails;
import com.godaddy.vps4.notifications.NotificationService;
import com.godaddy.vps4.notifications.NotificationFilterType;
import com.godaddy.vps4.util.NotificationListSearchFilters;
import com.godaddy.vps4.notifications.jdbc.JdbcNotificationService;
import com.godaddy.vps4.phase2.SqlTestData;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class JdbcNotificationServiceTest {
    static private Injector injectorForDS;
    private Injector injector;
    static private DataSource dataSource;
    private String imageName = "hfs-ubuntu-1604";

    @BeforeClass
    public static void setUpInternalInjector() {
        injectorForDS = Guice.createInjector(new DatabaseModule());
        dataSource = injectorForDS.getInstance(DataSource.class);
    }
    UUID notificationId = UUID.randomUUID();

    @Before
    public void setUp() {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DataSource.class).toInstance(dataSource);
                bind(NotificationService.class).to(JdbcNotificationService.class);
            }
        });

        List<NotificationFilter> filterList = new ArrayList<>();
        NotificationFilter filterImage = new NotificationFilter();
        filterImage.filterType = NotificationFilterType.IMAGE_ID;
        filterImage.filterValue = Arrays.asList("1000");
        NotificationFilter filterDC = new NotificationFilter();
        filterDC.filterType = NotificationFilterType.RESELLER_ID;
        filterDC.filterValue = Arrays.asList("2000");
        NotificationFilter filterPlatformId = new NotificationFilter();
        NotificationFilter filterHv = new NotificationFilter();
        filterHv.filterType = NotificationFilterType.HYPERVISOR_HOSTNAME;
        filterHv.filterValue = Arrays.asList("3000");
        NotificationFilter filterTier = new NotificationFilter();
        filterTier.filterType = NotificationFilterType.TIER;
        filterTier.filterValue = Arrays.asList("4000");
        filterPlatformId.filterType = NotificationFilterType.PLATFORM_ID;
        filterPlatformId.filterValue = Arrays.asList("5000");
        NotificationFilter filterVmId = new NotificationFilter();
        filterVmId.filterType = NotificationFilterType.VM_ID;
        filterVmId.filterValue = Arrays.asList("6000");

        filterList.add(filterDC);
        filterList.add(filterImage);
        filterList.add(filterPlatformId);
        filterList.add(filterTier);
        filterList.add(filterHv);
        filterList.add(filterVmId);

        SqlTestData.insertTestNotification(notificationId, NotificationType.PATCHING,true,true,
                null, null, null, null, filterList, dataSource);
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestNotification(notificationId, dataSource);
    }
    @Test
    public void createAndGetNotificationByNotificationIsOk() {
        assertNotNull(injector.getInstance(NotificationService.class).getNotification(notificationId));
    }

    @Test
    public void getNotificationsShowListOfNotificationsActiveOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byActive(true);
        searchFilters.byAdminView(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertTrue(notifications.size() > 0);
    }

    @Test
    public void testGetNotificationWithPastDateFilterOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byAdminView(true);
        searchFilters.byDateRange(Instant.parse("1997-08-13T10:15:30.345Z"), Instant.parse("1997-08-14T10:15:30.345Z"));

        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        Assert.assertEquals(0, notifications.size());
    }

    @Test
    public void testGetNotificationWithCurrentDateFilterOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byAdminView(true);
        searchFilters.byDateRange(Instant.now(), Instant.now());

        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertTrue(notifications.size() > 0);
    }

    @Test
    public void getNotificationsShowListOfNotificationsByImageIdOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();

        searchFilters.byImageId("1000");
        searchFilters.byAdminView(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());

        searchFilters.byImageId("2000");
        searchFilters.byAdminView(true);
        List<Notification> notificationsWrong = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertEquals(0, notificationsWrong.size());

    }

    @Test
    public void getNotificationsShowListOfNotificationsByResellerIdOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();

        searchFilters.byResellerId("2000");
        searchFilters.byAdminView(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());

        searchFilters.byResellerId("3000");
        searchFilters.byAdminView(true);
        List<Notification> notificationsWrong = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertEquals(0, notificationsWrong.size());
    }

    @Test
    public void getNotificationsShowListOfNotificationsByHvOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byHypervisors("3000");
        searchFilters.byAdminView(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());

        searchFilters.byHypervisors("4000");
        searchFilters.byAdminView(true);
        List<Notification> notificationsWrong = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertEquals(0, notificationsWrong.size());
    }

    @Test
    public void getNotificationsShowListOfNotificationsByTierOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byTier("4000");
        searchFilters.byAdminView(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());

        searchFilters.byTier("5000");
        searchFilters.byAdminView(true);
        List<Notification> notificationsWrong = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertEquals(0, notificationsWrong.size());
    }

    @Test
    public void getNotificationsShowListOfNotificationsByPlatformOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byPlatform("5000");
        searchFilters.byAdminView(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());

        searchFilters.byPlatform("6000");
        searchFilters.byAdminView(true);
        List<Notification> notificationsWrong = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertEquals(0, notificationsWrong.size());
    }

    @Test
    public void getNotificationsShowListOfNotificationsByVmIdOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byVmId("6000");
        searchFilters.byAdminView(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());

        searchFilters.byVmId("7000");
        searchFilters.byAdminView(true);
        List<Notification> notificationsWrong = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertEquals(0, notificationsWrong.size());
    }

    @Test
    public void getNotificationsShowListOfNotificationsByTypeOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byType(NotificationType.PATCHING);
        searchFilters.byTier("4000");
        searchFilters.byAdminView(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());

        searchFilters.byType(NotificationType.NEW_MESSAGE_CENTER);
        searchFilters.byTier("4000");
        searchFilters.byAdminView(true);
        List<Notification> notificationsWrong = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertEquals(0, notificationsWrong.size());
    }

    @Test
    public void getNotificationsShowListOfNotificationsByMultipleFiltersOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byTier("4000");
        searchFilters.byPlatform("5000");
        searchFilters.byVmId("6000");
        searchFilters.byAdminView(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());

        searchFilters.byTier("5000");
        searchFilters.byPlatform("6000");
        searchFilters.byVmId("7000");
        searchFilters.byAdminView(true);
        List<Notification> notificationsWrong = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertEquals(0, notificationsWrong.size());
    }

    @Test
    public void getNotificationsShowListOfNotificationsOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byActive(true);
        searchFilters.byAdminView(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertFalse(notifications.size() == 0);
    }

    @Test
    public void getNotificationsAdminViewFalse() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byAdminView(false);
        searchFilters.byResellerId(Arrays.asList("2000"));
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        Assert.assertEquals(0, notifications.size());
    }

    @Test
    public void getNotificationsAdminViewTrue() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byAdminView(true);
        searchFilters.byResellerId(Arrays.asList("2000"));
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        Assert.assertEquals(1, notifications.size());
    }

    @Test
    public void insertAndDeleteNotificationTest() {
        NotificationService service = injector.getInstance(NotificationService.class);
        UUID testNotificationId = UUID.randomUUID();
        List<NotificationFilter> filters = new ArrayList<>();
        NotificationFilter filter = new NotificationFilter();
        filter.filterType = NotificationFilterType.RESELLER_ID;
        filter.filterValue = Arrays.asList("1000");
        filters.add(filter);
        NotificationExtendedDetails notificationExtendedDetails = new NotificationExtendedDetails();
        notificationExtendedDetails.start = null;
        notificationExtendedDetails.end = null;
        service.createNotification(testNotificationId, NotificationType.PATCHING,true,true,
                notificationExtendedDetails, filters, null, null);
        assertNotNull(service.getNotification(testNotificationId));
        service.deleteNotification(testNotificationId);
        assertNull(service.getNotification(testNotificationId));
    }

    @Test
    public void updateNotificationTest() {
        NotificationService service = injector.getInstance(NotificationService.class);
        List<NotificationFilter> filters = new ArrayList<>();
        NotificationFilter filter = new NotificationFilter();
        filter.filterType = NotificationFilterType.IMAGE_ID;
        filter.filterValue = Arrays.asList("2000");
        filters.add(filter);
        NotificationExtendedDetails notificationExtendedDetails = new NotificationExtendedDetails();
        notificationExtendedDetails.start = Instant.now();
        notificationExtendedDetails.end = Instant.now();
        service.updateNotification(notificationId, NotificationType.MAINTENANCE,false,false,
                notificationExtendedDetails, filters, null, null);

        Notification modifiedNotification = service.getNotification(notificationId);
        assertEquals(NotificationType.MAINTENANCE, modifiedNotification.type);
        Assert.assertEquals(false, modifiedNotification.supportOnly);
        Assert.assertEquals(false, modifiedNotification.dismissible);
        assertNotNull(modifiedNotification.validOn);
        assertNotNull(modifiedNotification.validUntil);
        assertNotNull(modifiedNotification.notificationExtendedDetails.start);
        assertNotNull(modifiedNotification.notificationExtendedDetails.end);
        Assert.assertEquals(NotificationFilterType.IMAGE_ID, modifiedNotification.filters.get(0).filterType);
        Assert.assertEquals("2000", modifiedNotification.filters.get(0).filterValue.get(0));
    }

    @Test
    public void updateNotificationTestStartAndEndDateNull() {
        NotificationService service = injector.getInstance(NotificationService.class);
        List<NotificationFilter> filters = new ArrayList<>();
        NotificationFilter filter = new NotificationFilter();
        filter.filterType = NotificationFilterType.IMAGE_ID;
        filter.filterValue = Arrays.asList("2000");
        filters.add(filter);
        NotificationExtendedDetails notificationExtendedDetails = new NotificationExtendedDetails();
        notificationExtendedDetails.start = null;
        notificationExtendedDetails.end = null;
        service.updateNotification(notificationId, NotificationType.MAINTENANCE,false,false,
                notificationExtendedDetails, filters, null, null);

        Notification modifiedNotification = service.getNotification(notificationId);
        assertEquals(NotificationType.MAINTENANCE, modifiedNotification.type);
        Assert.assertEquals(false, modifiedNotification.supportOnly);
        Assert.assertEquals(false, modifiedNotification.dismissible);
        assertNull(modifiedNotification.notificationExtendedDetails.start);
        assertNull(modifiedNotification.notificationExtendedDetails.end);
        Assert.assertEquals(NotificationFilterType.IMAGE_ID, modifiedNotification.filters.get(0).filterType);
        Assert.assertEquals("2000", modifiedNotification.filters.get(0).filterValue.get(0));
    }

    @Test
    public void updateNotificationTestEndDateNullOnly() {
        NotificationService service = injector.getInstance(NotificationService.class);
        List<NotificationFilter> filters = new ArrayList<>();
        NotificationFilter filter = new NotificationFilter();
        filter.filterType = NotificationFilterType.IMAGE_ID;
        filter.filterValue = Arrays.asList("2000");
        filters.add(filter);
        NotificationExtendedDetails notificationExtendedDetails = new NotificationExtendedDetails();
        notificationExtendedDetails.start = Instant.now();
        notificationExtendedDetails.end = null;
        service.updateNotification(notificationId, NotificationType.MAINTENANCE,false,false,
                notificationExtendedDetails, filters, null, null);

        Notification modifiedNotification = service.getNotification(notificationId);
        assertEquals(NotificationType.MAINTENANCE, modifiedNotification.type);
        Assert.assertEquals(false, modifiedNotification.supportOnly);
        Assert.assertEquals(false, modifiedNotification.dismissible);
        assertNotNull(modifiedNotification.notificationExtendedDetails.start);
        assertNotNull(modifiedNotification.notificationExtendedDetails.end);
        Assert.assertEquals(NotificationFilterType.IMAGE_ID, modifiedNotification.filters.get(0).filterType);
        Assert.assertEquals("2000", modifiedNotification.filters.get(0).filterValue.get(0));
    }

    @Test
    public void getNotificationNotExistsReturnsNull() {
        NotificationService service = injector.getInstance(NotificationService.class);
        UUID notRealNotificationId = UUID.randomUUID();
        assertNull(service.getNotification(notRealNotificationId));
    }
    
    @Test
    public void getFilterTypesOk() {
        NotificationService service = injector.getInstance(NotificationService.class);
        List<NotificationFilterType> filters = service.getFilters();
        assertNotNull(filters);
        assertFalse(filters.isEmpty());
    }

    @Test
    public void addFilterToNotificationsOk() {
        NotificationService service = injector.getInstance(NotificationService.class);

        List<NotificationFilter> filters = new ArrayList<>();
        NotificationFilter filter = new NotificationFilter();
        filter.filterType = NotificationFilterType.RESELLER_ID;
        filter.filterValue = Arrays.asList("4000");
        filters.add(filter);
        service.addFilterToNotification(notificationId, filters);

        Notification modifiedNotification = service.getNotification(notificationId);
        List<NotificationFilter> retrievedFilters = modifiedNotification.filters;

        for(NotificationFilter eachFilter : retrievedFilters) {
            if(eachFilter.filterType == NotificationFilterType.RESELLER_ID)
            {
                assertEquals(Arrays.asList("4000"), eachFilter.filterValue);
                return;
            }
        }

        fail("Should not have reached here.");
    }
}
