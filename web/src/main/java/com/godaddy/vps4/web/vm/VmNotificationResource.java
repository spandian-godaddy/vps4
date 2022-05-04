package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnDateInstant;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.notifications.Notification;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.notification.NotificationsResource;
import com.godaddy.vps4.web.security.GDUser;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmNotificationResource {
    private static final Logger logger = LoggerFactory.getLogger(VmNotificationResource.class);

    private final NotificationsResource notificationsResource;
    private final VmResource vmResource;
    private final CreditService creditService;
    private final VirtualMachineService virtualMachineService;
    private final GDUser user;

    @Inject
    public VmNotificationResource(NotificationsResource notificationsResource, VirtualMachineService virtualMachineService,
                                  CreditService creditService, VmResource vmResource, GDUser user) {
        this.creditService = creditService;
        this.vmResource = vmResource;
        this.notificationsResource = notificationsResource;
        this.virtualMachineService = virtualMachineService;
        this.user = user;
    }

    private boolean isImported(UUID vmId) {
        return virtualMachineService.getImportedVm(vmId) != null;
    }

    @GET
    @Path("/{vmId}/notifications/")
    public List<Notification> getVmNotifications(@PathParam("vmId") UUID vmId,
                                                 @ApiParam(value = "begin date in UTC, Example: 2007-12-03T10:15:30.00Z", required = false) @QueryParam("beginDate") String beginDate,
                                                 @ApiParam(value = "end date in UTC, Example: 2007-12-03T10:15:30.00Z", required = false) @QueryParam("endDate") String endDate) {
        logger.info("Getting notifications for vmId:  {}", vmId);
        Instant validOnDate = beginDate == null ? Instant.now() : validateAndReturnDateInstant(beginDate);
        Instant validUntilDate = endDate == null ? Instant.now() : validateAndReturnDateInstant(endDate);
        boolean isSupport = user.roles().contains(GDUser.Role.HS_AGENT) ||
                user.roles().contains(GDUser.Role.ADMIN) ||
                user.roles().contains(GDUser.Role.HS_LEAD);

        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        List<Notification> listNotifications;

        VirtualMachineCredit virtualMachineCredit = creditService.getVirtualMachineCredit(virtualMachine.orionGuid);
        String hypervisorHostname = null;
        if (!virtualMachineCredit.isDed4()) {
            VmExtendedInfo vmExtendedInfo = vmResource.getVmExtendedInfoFromVmVertical(virtualMachine.hfsVmId);
            if (vmExtendedInfo != null) hypervisorHostname = vmExtendedInfo.extended.hypervisorHostname;
        }
        try {
            listNotifications = notificationsResource.getNotificationsBasedOnFilters(
                    Arrays.asList(Long.toString(virtualMachine.image.imageId)),
                    Arrays.asList(virtualMachineCredit.getResellerId()),
                    Collections.emptyList(),
                    validOnDate.toString(),
                    validUntilDate.toString(),
                    hypervisorHostname == null ? Collections.emptyList() : Arrays.asList(hypervisorHostname),
                    Arrays.asList(Integer.toString(virtualMachine.spec.tier)),
                    Arrays.asList(Integer.toString(virtualMachine.image.serverType.platform.getplatformId())),
                    Arrays.asList((virtualMachine.vmId).toString()),
                    false,
                    virtualMachineCredit.isManaged(),
                    isImported(vmId),
                    isSupport
            );
        }
        catch (Exception ex) {
            logger.warn("Exception encountered when attempting to get notifications for vmId:  {} , Exception: {} ", vmId, ex);
            throw ex;
        }
        return listNotifications;
    }
}
