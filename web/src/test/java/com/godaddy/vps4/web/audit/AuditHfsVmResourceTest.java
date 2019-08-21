package com.godaddy.vps4.web.audit;

import static com.godaddy.vps4.hfs.HfsVmTrackingRecordService.ListFilters;
import static com.godaddy.vps4.hfs.HfsVmTrackingRecordService.Status;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;

public class AuditHfsVmResourceTest {

    @Captor
    private ArgumentCaptor<ListFilters> listFiltersArgumentCaptor;

    HfsVmTrackingRecordService service = mock(HfsVmTrackingRecordService.class);
    AuditHfsVmResource resource = new AuditHfsVmResource(service);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getByStatus() {
        resource.getHfsVmTrackingRecords(AuditHfsVmResource.Status.UNUSED, null, 0, null);
        verify(service, times(1)).getTrackingRecords(listFiltersArgumentCaptor.capture());
        ListFilters listFilters = listFiltersArgumentCaptor.getValue();
        assertEquals(Status.UNUSED, listFilters.byStatus);
        assertNull(listFilters.vmId);
        assertNull(listFilters.sgid);
        assertEquals(0, listFilters.hfsVmId);
    }

    @Test
    public void getByVps4VmId() {
        UUID vmId = UUID.randomUUID();
        resource.getHfsVmTrackingRecords(null, vmId, 0, null);
        verify(service, times(1)).getTrackingRecords(listFiltersArgumentCaptor.capture());
        ListFilters listFilters = listFiltersArgumentCaptor.getValue();
        assertEquals(vmId, listFilters.vmId);
        assertNull(listFilters.byStatus);
        assertNull(listFilters.sgid);
        assertEquals(0, listFilters.hfsVmId);
    }

    @Test
    public void getByHFSVmId() {
        long hfsVmId = 123L;
        resource.getHfsVmTrackingRecords(null, null, hfsVmId, null);
        verify(service, times(1)).getTrackingRecords(listFiltersArgumentCaptor.capture());
        ListFilters listFilters = listFiltersArgumentCaptor.getValue();
        assertEquals(hfsVmId, listFilters.hfsVmId);
        assertNull(listFilters.vmId);
        assertNull(listFilters.byStatus);
        assertNull(listFilters.sgid);
    }

    @Test
    public void getBySGID() {
        String sgid = "FOOBAR";
        resource.getHfsVmTrackingRecords(null, null, 0, sgid);
        verify(service, times(1)).getTrackingRecords(listFiltersArgumentCaptor.capture());
        ListFilters listFilters = listFiltersArgumentCaptor.getValue();
        assertEquals(sgid, listFilters.sgid);
        assertNull(listFilters.vmId);
        assertNull(listFilters.byStatus);
        assertEquals(0, listFilters.hfsVmId);
    }

    @Test
    public void getByAllParams() {
        UUID vmId = UUID.randomUUID();
        long hfsVmId = 123L;
        String sgid = "FOOBAR";
        resource.getHfsVmTrackingRecords(AuditHfsVmResource.Status.UNUSED, vmId, hfsVmId, sgid);
        verify(service, times(1)).getTrackingRecords(listFiltersArgumentCaptor.capture());
        ListFilters listFilters = listFiltersArgumentCaptor.getValue();
        assertEquals(sgid, listFilters.sgid);
        assertEquals(hfsVmId, listFilters.hfsVmId);
        assertEquals(Status.UNUSED, listFilters.byStatus);
        assertEquals(vmId, listFilters.vmId);
    }

}
