package com.godaddy.vps4.phase2;

import com.godaddy.hfs.vm.Extended;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.notifications.Notification;
import com.godaddy.vps4.notifications.NotificationFilter;
import com.godaddy.vps4.notifications.NotificationFilterType;
import com.godaddy.vps4.notifications.NotificationType;
import com.godaddy.vps4.notifications.NotificationExtendedDetails;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.web.notification.NotificationsModule;
import com.godaddy.vps4.web.notification.NotificationsResource;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Inject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationsResourceTest {
    private VmResource vmResource = mock(VmResource.class);
    private VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    private CreditService creditService = mock(CreditService.class);

    @Inject
    DataSource dataSource;

    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
    private Long hfsVmId = 42L;
    private UUID notificationId;
    private VirtualMachine vps4Vm = new VirtualMachine();
    private VmExtendedInfo vmExtendedInfoMock = new VmExtendedInfo();
    private Injector injector = Guice.createInjector(new DatabaseModule(),
            new SecurityModule(),
            new NotificationsModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(VmResource.class).toInstance(vmResource);
                    bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
                }
            });

    private NotificationsResource getNotificationResource() {
        return injector.getInstance(NotificationsResource.class);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        injector.injectMembers(this);
        vps4Vm.hfsVmId = hfsVmId;
        vps4Vm.orionGuid = orionGuid;
        vps4Vm.canceled = Instant.MAX;
        when(vmResource.getVm(vmId)).thenReturn(vps4Vm);

        Vm hfsVm = new Vm();
        hfsVm.vmId = hfsVmId;
        hfsVm.status = "ACTIVE";
        hfsVm.running = true;
        hfsVm.useable = true;
        hfsVm.resourceId = "ns3210123.ip-123-45-67.eu";
        when(vmResource.getVmFromVmVertical(hfsVmId)).thenReturn(hfsVm);

        vmExtendedInfoMock.provider = "nocfox";
        vmExtendedInfoMock.resource = "openstack";
        Extended extendedMock = new Extended();
        extendedMock.hypervisorHostname = "n3plztncldhv001-02.prod.ams3.gdg";
        vmExtendedInfoMock.extended = extendedMock;
        when(vmResource.getVmExtendedInfoFromVmVertical(hfsVmId)).thenReturn(vmExtendedInfoMock);
        credit = mock(VirtualMachineCredit.class);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);


        List<NotificationFilter> filterList = new ArrayList<>();
        NotificationFilter filterDC = new NotificationFilter();
        filterDC.filterType = NotificationFilterType.RESELLER_ID;
        filterDC.filterValue = Arrays.asList("100");
        NotificationFilter filterImage = new NotificationFilter();
        filterImage.filterType = NotificationFilterType.IMAGE_ID;
        filterImage.filterValue = Arrays.asList("100");
        NotificationFilter filterPlatformId = new NotificationFilter();
        filterPlatformId.filterType = NotificationFilterType.PLATFORM_ID;
        filterPlatformId.filterValue = Arrays.asList("100");

        filterList.add(filterDC);
        filterList.add(filterImage);

        notificationId = createTestNotification(filterList);
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    @Test
    public void testGetNotificationWithFilter() {
        List<String> statusList = Arrays.asList("100");

        List<String> statusListWrong = Arrays.asList("200");
        List<String> emptyList = new ArrayList<>();

        List<Notification> notifications = getNotificationResource().getNotificationsBasedOnFilters(statusList,
                statusList,emptyList,null,null,emptyList,emptyList,statusList,emptyList,true);
        Assert.assertEquals(1, notifications.size());
        List<Notification> notificationsWrong = getNotificationResource().getNotificationsBasedOnFilters( statusListWrong,
                statusListWrong,Arrays.asList(NotificationType.NEW_FEATURE_OPCACHE.name()),null,null,emptyList,
                emptyList,statusListWrong,emptyList,true);
        Assert.assertEquals(0, notificationsWrong.size());
    }

    @Test
    public void testGetNotificationWithNotificationId() {
        Notification notification = getNotificationResource().getNotification(notificationId);
        Assert.assertEquals(notificationId, notification.notificationId);
    }

    @Test
    public void testAddAndDeleteNotification() {
        NotificationsResource.NotificationRequest request = new NotificationsResource.NotificationRequest();
        request.type = NotificationType.PATCHING;
        request.dismissible = true;
        request.supportOnly = true;
        NotificationExtendedDetails notificationExtendedDetails = new NotificationExtendedDetails();
        notificationExtendedDetails.end = null;
        notificationExtendedDetails.start = null;
        request.notificationExtendedDetails = notificationExtendedDetails;
        NotificationFilter filter = new NotificationFilter();
        filter.filterValue = Arrays.asList("1","2");
        filter.filterType = NotificationFilterType.RESELLER_ID;
        request.filters = Arrays.asList(filter);
        UUID notificationId = getNotificationResource().createNewNotification(request).notificationId;
        Notification createdNotification = getNotificationResource().getNotification(notificationId);
        Assert.assertEquals(createdNotification.notificationId, notificationId);
        Assert.assertEquals(createdNotification.type, NotificationType.PATCHING);
        Assert.assertEquals(createdNotification.dismissible, true);
        Assert.assertEquals(createdNotification.supportOnly, true);

        getNotificationResource().deleteNotifications(notificationId);
        Notification deletedNotification = getNotificationResource().getNotification(notificationId);
        assertNull(deletedNotification);
    }

    @Test
    public void testUpdateNotification() {
        NotificationsResource.NotificationRequest request = new NotificationsResource.NotificationRequest();
        request.type = NotificationType.MAINTENANCE;
        request.dismissible = false;
        request.supportOnly = false;
        NotificationExtendedDetails notificationExtendedDetails = new NotificationExtendedDetails();
        notificationExtendedDetails.end = Instant.now();
        notificationExtendedDetails.start = Instant.now();
        request.notificationExtendedDetails = notificationExtendedDetails;
        NotificationFilter filter = new NotificationFilter();
        filter.filterValue = Arrays.asList("2","3");
        filter.filterType = NotificationFilterType.RESELLER_ID;
        request.filters = Arrays.asList(filter);

        getNotificationResource().putNotification(notificationId,request);
        Notification modifiedNotification = getNotificationResource().getNotification(notificationId);
        Assert.assertEquals(notificationId, modifiedNotification.notificationId);
        Assert.assertEquals(NotificationType.MAINTENANCE, modifiedNotification.type);
        Assert.assertEquals(false, modifiedNotification.supportOnly);
        Assert.assertEquals(false, modifiedNotification.dismissible);
        assertNotNull(modifiedNotification.notificationExtendedDetails.start);
        assertNotNull(modifiedNotification.notificationExtendedDetails.end);
        Assert.assertEquals(1, modifiedNotification.filters.size());
    }

    @Test
    public void testGetFiltersReturnOK() {
        List<NotificationFilterType> filters = getNotificationResource().getFilters();
        Assert.assertNotNull(filters);
        Assert.assertFalse(filters.size() == 0);
    }

    private UUID createTestNotification(List<NotificationFilter> filters) {
        UUID notificationId = SqlTestData.notificationId;
        SqlTestData.insertTestNotification(notificationId, NotificationType.PATCHING,true,
                true,null,null, filters, dataSource);
        return notificationId;
    }

}
