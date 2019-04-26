package com.godaddy.vps4.web.audit;

import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AuditHfsVmResourceTest {

    HfsVmTrackingRecordService service = mock(HfsVmTrackingRecordService.class);
    AuditHfsVmResource resource = new AuditHfsVmResource(service);

    @Test
    public void testGetUnusedHfsVms() {
        resource.getHfsVmTrackingRecords(AuditHfsVmResource.Status.UNUSED);
        verify(service, times(1)).getUnused();
    }

    @Test
    public void testGetCanceledHfsVms() {
        resource.getHfsVmTrackingRecords(AuditHfsVmResource.Status.CANCELED);
        verify(service, times(1)).getCanceled();
    }

    @Test
    public void testGetRequestedHfsVms() {
        resource.getHfsVmTrackingRecords(AuditHfsVmResource.Status.REQUESTED);
        verify(service, times(1)).getRequested();
    }

}
