package com.godaddy.vps4.web.audit;

import static com.godaddy.vps4.hfs.HfsVmTrackingRecordService.ListFilters;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.hfs.HfsVmTrackingRecord;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


@Vps4Api
@Api(tags = {"audit"})
@Path("/api/audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditHfsVmResource {

    private HfsVmTrackingRecordService hfsVmTrackingRecordService;

    public enum Status {
        UNUSED, CANCELED, REQUESTED
    }

    @Inject
    public AuditHfsVmResource(HfsVmTrackingRecordService hfsVmTrackingRecordService) {
        this.hfsVmTrackingRecordService = hfsVmTrackingRecordService;
    }

    @GET
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @Path("/hfsvms")
    @ApiOperation(value = "Get HFS VM records.")
    public List<HfsVmTrackingRecord> getHfsVmTrackingRecords(
            @ApiParam(value = "The status of the HFS VM Tracking Record") @QueryParam
                    ("status") Status status,
            @ApiParam(value = "Vps4 vm id") @QueryParam("vmId") UUID vmId,
            @ApiParam(value = "Hfs vm id") @QueryParam("hfsVmId") long hfsVmId,
            @ApiParam(value = "SGID that the VM was created under") @QueryParam("sgid") String sgid) {
        ListFilters listFilters = new ListFilters();
        if (status != null) {
            listFilters.byStatus = HfsVmTrackingRecordService.Status.valueOf(status.toString());
        }
        listFilters.vmId = vmId;
        listFilters.hfsVmId = hfsVmId;
        listFilters.sgid = sgid;
        return hfsVmTrackingRecordService.getTrackingRecords(listFilters);
    }
}
