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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


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
        List<NotificationFilter> filters = new ArrayList<>();
        NotificationFilter filter = new NotificationFilter();
        filter.filterType = NotificationFilterType.RESELLER_ID;
        filter.filterValue = Arrays.asList("100");
        filters.add(filter);
        SqlTestData.insertTestNotification(notificationId, NotificationType.PATCHING,true,true,
                null, null, null, null, filters, dataSource);
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
    public void getNotificationsShowListOfNotificationsOk() {
        NotificationListSearchFilters searchFilters = new NotificationListSearchFilters();
        searchFilters.byActive(true);
        List<Notification> notifications = injector.getInstance(NotificationService.class).getNotifications(searchFilters);
        assertNotNull(notifications);
        assertFalse(notifications.size() == 0);
    }

    @Test
    public void insertAndDeleteNotificationTest() {
        NotificationService service = injector.getInstance(NotificationService.class);
        UUID testNotificationId = UUID.randomUUID();
        List<NotificationFilter> filters = new ArrayList<>();
        NotificationFilter filter = new NotificationFilter();
        filter.filterType = NotificationFilterType.RESELLER_ID;
        filter.filterValue = Arrays.asList("100");
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
        filter.filterValue = Arrays.asList("200");
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
        Assert.assertEquals("200", modifiedNotification.filters.get(0).filterValue.get(0));
    }

    @Test
    public void updateNotificationTestStartAndEndDateNull() {
        NotificationService service = injector.getInstance(NotificationService.class);
        List<NotificationFilter> filters = new ArrayList<>();
        NotificationFilter filter = new NotificationFilter();
        filter.filterType = NotificationFilterType.IMAGE_ID;
        filter.filterValue = Arrays.asList("200");
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
        Assert.assertEquals("200", modifiedNotification.filters.get(0).filterValue.get(0));
    }

    @Test
    public void updateNotificationTestEndNullOnly() {
        NotificationService service = injector.getInstance(NotificationService.class);
        List<NotificationFilter> filters = new ArrayList<>();
        NotificationFilter filter = new NotificationFilter();
        filter.filterType = NotificationFilterType.IMAGE_ID;
        filter.filterValue = Arrays.asList("200");
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
        Assert.assertEquals("200", modifiedNotification.filters.get(0).filterValue.get(0));
    }

    @Test
    public void getNotificationNotExistsReturnsNull() {
        NotificationService service = injector.getInstance(NotificationService.class);
        UUID notRealNotificationId = UUID.randomUUID();
        assertNull(service.getNotification(notRealNotificationId));
    }
}
