package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.vm.Extended;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.notifications.Notification;
import com.godaddy.vps4.notifications.NotificationFilter;
import com.godaddy.vps4.notifications.NotificationFilterType;
import com.godaddy.vps4.notifications.NotificationExtendedDetails;
import com.godaddy.vps4.notifications.NotificationService;
import com.godaddy.vps4.notifications.jdbc.JdbcNotificationService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.notification.NotificationsModule;
import com.godaddy.vps4.web.notification.NotificationsResource;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyLong;

public class VmNotificationResourceTest {
    private NotificationsResource notificationsResource = mock(NotificationsResource.class);
    private VmResource vmResource = mock(VmResource.class);
    private VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    private VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    private CreditService creditService = mock(CreditService.class);
    private NotificationService notificationService = mock(NotificationService.class);
    private GDUser user = mock(GDUser.class);
    private VirtualMachineCredit dedCredit;
    private HashMap<String,String> planFeaturesDed = new HashMap<>();
    private HashMap<String,String> planFeatures = new HashMap<>();
    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
    private Long hfsVmId = 42L;
    private VirtualMachine vps4Vm = new VirtualMachine();
    private VmExtendedInfo vmExtendedInfoMock = new VmExtendedInfo();
    private Notification notification = new Notification();

    private Injector injector = Guice.createInjector(new DatabaseModule(),
            new SecurityModule(),
            new NotificationsModule(),
            new AbstractModule() {
                @Override
                public void configure() {
                    bind(NotificationsResource.class).toInstance(notificationsResource);
                    bind(VmResource.class).toInstance(vmResource);
                    bind(NotificationService.class).to(JdbcNotificationService.class);
                    bind(CreditService.class).toInstance(creditService);
                    bind(VirtualMachineService.class).toInstance(virtualMachineService);
                }
                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    private VmNotificationResource getVmNotificationResource() {
        return injector.getInstance(VmNotificationResource.class);
    }

    private NotificationsResource getNotificationsResource() {
        return injector.getInstance(NotificationsResource.class);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        injector.injectMembers(this);
        vps4Vm.hfsVmId = hfsVmId;
        vps4Vm.orionGuid = orionGuid;
        vps4Vm.canceled = Instant.MAX;
        vps4Vm.vmId = vmId;
        Image image = new Image();
        image.imageId = 100;
        ServerType serverType = new ServerType();
        serverType.platform = ServerType.Platform.OPENSTACK;
        image.serverType = serverType;
        vps4Vm.image = image;
        ServerSpec spec = new ServerSpec();
        spec.tier = 30;
        vps4Vm.spec = spec;

        Vm hfsVm = new Vm();
        hfsVm.vmId = hfsVmId;
        hfsVm.status = "ACTIVE";
        hfsVm.running = true;
        hfsVm.useable = true;
        hfsVm.resourceId = "ns3210123.ip-123-45-67.eu";

        vmExtendedInfoMock.provider = "nocfox";
        vmExtendedInfoMock.resource = "openstack";
        Extended extendedMock = new Extended();
        extendedMock.hypervisorHostname = "n3plztncldhv001-02.prod.ams3.gdg";
        vmExtendedInfoMock.extended = extendedMock;
        NotificationExtendedDetails notificationExtendedDetails = new NotificationExtendedDetails();
        notificationExtendedDetails.start = null;
        notificationExtendedDetails.end = null;
        NotificationFilter filter = new NotificationFilter();
        filter.filterValue = Arrays.asList("30");
        filter.filterType = NotificationFilterType.TIER;
        notification.notificationExtendedDetails = notificationExtendedDetails;
        notification.notificationId = UUID.randomUUID();
        notification.validOn = Instant.now();
        notification.validUntil = Instant.MAX;
        notification.dismissible = true;
        notification.supportOnly = false;
        notification.filters = Arrays.asList(filter);

        planFeaturesDed.put(ECommCreditService.PlanFeatures.TIER.toString(), "70");
        planFeaturesDed.put(ECommCreditService.PlanFeatures.MANAGED_LEVEL.toString(), "1");
        planFeatures.put(ECommCreditService.PlanFeatures.TIER.toString(), "20");
        planFeatures.put(ECommCreditService.PlanFeatures.MANAGED_LEVEL.toString(), "1");
        dedCredit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(UUID.randomUUID().toString())
                .withPlanFeatures(planFeaturesDed)
                .withResellerID("1")
                .build();
        credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(UUID.randomUUID().toString())
                .withPlanFeatures(planFeatures)
                .withResellerID("1")
                .build();

        when(vmResource.getVm(vmId)).thenReturn(vps4Vm);
        when(vmResource.getVmFromVmVertical(hfsVmId)).thenReturn(hfsVm);
        when(notificationService.getNotifications(anyObject())).thenReturn(null);
        when(getNotificationsResource().getNotificationsBasedOnFilters
                (eq(Arrays.asList(Long.toString(vps4Vm.image.imageId))),
                        anyList(),
                        any(), 
                        anyObject(),
                        anyObject(),
                        anyList(),
                        eq(Arrays.asList(Integer.toString(vps4Vm.spec.tier))),
                        eq(Arrays.asList(Integer.toString(vps4Vm.image.serverType.platform.getplatformId()))),
                        eq(Arrays.asList(vps4Vm.vmId.toString())),
                        anyBoolean(),
                        eq(credit.isManaged()),
                        anyBoolean())).thenReturn(Arrays.asList(notification));
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);
        when(vmResource.getVmExtendedInfoFromVmVertical(hfsVmId)).thenReturn(vmExtendedInfoMock);
        when(user.roles()).thenReturn(Arrays.asList(GDUser.Role.CUSTOMER));
    }

    @Test
    public void verifyNotificationResourceIsCalled() {
        getVmNotificationResource().getVmNotifications(vmId, "2008-08-05T23:55:02.162126Z",
                "3021-08-05T23:55:02.162126Z");
        verify(vmResource).getVmExtendedInfoFromVmVertical(anyLong());
        verify(notificationsResource).getNotificationsBasedOnFilters(
                eq(Arrays.asList(Long.toString(vps4Vm.image.imageId))),
                anyList(),
                any(),
                anyObject(),
                anyObject(),
                anyList(),
                eq(Arrays.asList(Integer.toString(vps4Vm.spec.tier))),
                eq(Arrays.asList(Integer.toString(vps4Vm.image.serverType.platform.getplatformId()))),
                eq(Arrays.asList(vps4Vm.vmId.toString())),
                anyBoolean(),
                eq(credit.isManaged()),
                anyBoolean());
    }

    @Test
    public void testGetNotificationOfVmSameTier() {
        List<Notification> notifications = getVmNotificationResource().getVmNotifications(vmId, "2008-08-05T23:55:02.162126Z",
                "3021-08-05T23:55:02.162126Z");
        assertEquals(1, notifications.size());
    }

    @Test
    public void testGetNotificationCallsCustomerView() {
        when(user.roles()).thenReturn(Arrays.asList(GDUser.Role.CUSTOMER));

        getVmNotificationResource().getVmNotifications(vmId, "2008-08-05T23:55:02.162126Z",
                "3021-08-05T23:55:02.162126Z");
        verify(notificationsResource).getNotificationsBasedOnFilters(
                eq(Arrays.asList(Long.toString(vps4Vm.image.imageId))),
                anyList(),
                any(), 
                anyObject(),
                anyObject(),
                anyList(),
                eq(Arrays.asList(Integer.toString(vps4Vm.spec.tier))),
                eq(Arrays.asList(Integer.toString(vps4Vm.image.serverType.platform.getplatformId()))),
                eq(Arrays.asList(vps4Vm.vmId.toString())),
                anyBoolean(),
                eq(credit.isManaged()),
                eq(false));
    }


    @Test
    public void testGetNotificationCallsAdminViewForAdmin() {
        when(user.roles()).thenReturn(Arrays.asList(GDUser.Role.ADMIN));

        getVmNotificationResource().getVmNotifications(vmId, "2008-08-05T23:55:02.162126Z",
                "3021-08-05T23:55:02.162126Z");
        verify(notificationsResource).getNotificationsBasedOnFilters(
                eq(Arrays.asList(Long.toString(vps4Vm.image.imageId))),
                anyList(),
                any(), 
                anyObject(),
                anyObject(),
                anyList(),
                eq(Arrays.asList(Integer.toString(vps4Vm.spec.tier))),
                eq(Arrays.asList(Integer.toString(vps4Vm.image.serverType.platform.getplatformId()))),
                eq(Arrays.asList(vps4Vm.vmId.toString())),
                anyBoolean(),
                eq(credit.isManaged()),
                eq(true));
    }



    @Test
    public void testGetNotificationCallsAdminViewForHS() {
        when(user.roles()).thenReturn(Arrays.asList(GDUser.Role.HS_AGENT));

        getVmNotificationResource().getVmNotifications(vmId, "2008-08-05T23:55:02.162126Z",
                "3021-08-05T23:55:02.162126Z");
        verify(notificationsResource).getNotificationsBasedOnFilters(
                eq(Arrays.asList(Long.toString(vps4Vm.image.imageId))),
                anyList(),
                any(), 
                anyObject(),
                anyObject(),
                anyList(),
                eq(Arrays.asList(Integer.toString(vps4Vm.spec.tier))),
                eq(Arrays.asList(Integer.toString(vps4Vm.image.serverType.platform.getplatformId()))),
                eq(Arrays.asList(vps4Vm.vmId.toString())),
                anyBoolean(),
                eq(credit.isManaged()),
                eq(true));
    }


    @Test
    public void testIfDed4DoNotPullHypervisor() {
        when(creditService.getVirtualMachineCredit(any())).thenReturn(dedCredit);
        getVmNotificationResource().getVmNotifications(vmId, "2008-08-05T23:55:02.162126Z",
                "3021-08-05T23:55:02.162126Z");
        verify(vmResource, never()).getVmExtendedInfoFromVmVertical(anyLong());
        verify(notificationsResource).getNotificationsBasedOnFilters(
                eq(Arrays.asList(Long.toString(vps4Vm.image.imageId))),
                anyList(),
                any(),
                anyObject(),
                anyObject(),
                eq(Collections.emptyList()),
                eq(Arrays.asList(Integer.toString(vps4Vm.spec.tier))),
                eq(Arrays.asList(Integer.toString(vps4Vm.image.serverType.platform.getplatformId()))),
                eq(Arrays.asList(vps4Vm.vmId.toString())),
                anyBoolean(),
                eq(dedCredit.isManaged()),
                anyBoolean());
    }
}
