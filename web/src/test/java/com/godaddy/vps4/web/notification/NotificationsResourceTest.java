package com.godaddy.vps4.web.notification;

import com.godaddy.hfs.vm.Extended;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.notifications.Notification;
import com.godaddy.vps4.notifications.NotificationExtendedDetails;
import com.godaddy.vps4.notifications.NotificationFilter;
import com.godaddy.vps4.notifications.NotificationFilterType;
import com.godaddy.vps4.notifications.NotificationService;
import com.godaddy.vps4.notifications.NotificationType;
import com.godaddy.vps4.phase2.Phase2ExternalsModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.util.NotificationListSearchFilters;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotificationsResourceTest {
    private VmResource vmResource = mock(VmResource.class);
    private VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    private CreditService creditService = mock(CreditService.class);
    private NotificationService notificationService = mock(NotificationService.class);

    @Captor
    private ArgumentCaptor<NotificationListSearchFilters> notificationListSearchFiltersArgumentCaptor;

    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
    private Long hfsVmId = 42L;
    private VirtualMachine vps4Vm = new VirtualMachine();
    private VmExtendedInfo vmExtendedInfoMock = new VmExtendedInfo();
    private Injector injector = Guice.createInjector(new DatabaseModule(),
            new SecurityModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(VmResource.class).toInstance(vmResource);
                    bind(NotificationService.class).toInstance(notificationService);
                    bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
                }
            });

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
    }

    private NotificationsResource getNotificationResource() {
        return injector.getInstance(NotificationsResource.class);
    }

    @Test
    public void testGetNotificationCallsNotificationService() {
        List<String> filterList = Arrays.asList("1000");
        List<String> emptyList = new ArrayList<>();

        getNotificationResource().getNotificationsBasedOnFilters(filterList,
                filterList,emptyList,null,null,emptyList,emptyList,filterList,emptyList, false, null,true);

        verify(notificationService,times(1)).getNotifications(anyObject());
    }

    @Test
    public void testGetNotificationCallsNotificationServiceWithCorrectArguments() {
        List<String> filterList = Arrays.asList("1000");
        List<String> emptyList = new ArrayList<>();
        getNotificationResource().getNotificationsBasedOnFilters(filterList,
                filterList,emptyList,null,null,emptyList,filterList,emptyList,emptyList,false,null,true);

        verify(notificationService,times(1)).getNotifications(notificationListSearchFiltersArgumentCaptor.capture());
        NotificationListSearchFilters notificationListSearchFilters = notificationListSearchFiltersArgumentCaptor.getValue();
        assertEquals("1000", notificationListSearchFilters.getImageIds().get(0));
        assertEquals("1000", notificationListSearchFilters.getResellers().get(0));
        assertEquals("1000", notificationListSearchFilters.getTiers().get(0));
        assertEquals(0, notificationListSearchFilters.getHypervisor().size());
        assertEquals(0, notificationListSearchFilters.getTypeList().size());
        assertEquals(0, notificationListSearchFilters.getVmIds().size());
    }

    @Test
    public void testGetFiltersCallsServiceGetFilter() {
        List<NotificationFilterType> filters = getNotificationResource().getFilters();
        verify(notificationService,times(1)).getFilters();
    }

    @Test
    public void testAddNotificationCallsNotificationService() {
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
        List filterLists = Arrays.asList(filter);
        request.filters = filterLists;
        getNotificationResource().createNewNotification(request);
        verify(notificationService, times(1)).createNotification(anyObject(),
                eq(NotificationType.PATCHING), eq(true), eq(true), eq(notificationExtendedDetails), eq(filterLists), anyObject(), anyObject());
    }

    @Test
    public void testGetNotificationByNotificationIdCallsNotificationService() {
        UUID notificationId = UUID.randomUUID();
        getNotificationResource().getNotification(notificationId);
        verify(notificationService, times(1)).getNotification(eq(notificationId));
    }


    @Test
    public void testDeleteNotificationCallsNotificationService() {
        UUID notificationId = UUID.randomUUID();
        getNotificationResource().deleteNotifications(notificationId);
        verify(notificationService, times(1)).deleteNotification(eq(notificationId));
    }

    @Test(expected= NotFoundException.class)
    public void testPutNotificationThrowsExceptionFOrUnknownNotification() {
        UUID notificationId = UUID.randomUUID();

        NotificationsResource.NotificationRequest request = new NotificationsResource.NotificationRequest();
        request.type = NotificationType.MAINTENANCE;
        request.dismissible = true;
        request.supportOnly = true;

        getNotificationResource().putNotification(notificationId, request);
        verify(notificationService, times(1)).updateNotification(eq(notificationId), eq(NotificationType.MAINTENANCE),
                eq(true), eq(true), anyObject(), anyObject(), anyObject(), anyObject());
    }

    @Test
    public void testPutNotificationCallsNotificationService() {
        UUID notificationId = UUID.randomUUID();

        NotificationsResource.NotificationRequest request = new NotificationsResource.NotificationRequest();
        request.type = NotificationType.MAINTENANCE;
        request.dismissible = true;
        request.supportOnly = true;
        NotificationExtendedDetails notificationExtendedDetails = new NotificationExtendedDetails();
        notificationExtendedDetails.end = null;
        notificationExtendedDetails.start = null;
        request.notificationExtendedDetails = notificationExtendedDetails;
        NotificationFilter filter = new NotificationFilter();
        filter.filterValue = Arrays.asList("4","2");
        filter.filterType = NotificationFilterType.VM_ID;
        List filterLists = Arrays.asList(filter);
        request.filters = filterLists;
        when(notificationService.getNotification(anyObject())).thenReturn(new Notification());

        getNotificationResource().putNotification(notificationId, request);
        verify(notificationService, times(1)).updateNotification(eq(notificationId), eq(NotificationType.MAINTENANCE),
                eq(true), eq(true), eq(notificationExtendedDetails), eq(filterLists), anyObject(), anyObject());
    }

    @Test
    public void testAddNotificationFiltertoNotificationCallsNotificationService() {
        NotificationsResource.NotificationFilterRequest notificationFilterRequest = new NotificationsResource.NotificationFilterRequest();
        notificationFilterRequest.notificationId = UUID.randomUUID();
        NotificationFilter filter = new NotificationFilter();
        filter.filterValue = Arrays.asList("4","2");
        filter.filterType = NotificationFilterType.VM_ID;
        List filterLists = Arrays.asList(filter);
        notificationFilterRequest.filters = filterLists;
        getNotificationResource().addFilterToNotification(notificationFilterRequest);
        verify(notificationService, times(1)).addFilterToNotification(eq(notificationFilterRequest.notificationId), eq(filterLists));
    }
}
