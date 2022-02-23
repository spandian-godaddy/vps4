package com.godaddy.vps4.web.customNotes;
import com.godaddy.vps4.customNotes.CustomNote;
import com.godaddy.vps4.customNotes.CustomNotesService;
import com.godaddy.vps4.notifications.NotificationExtendedDetails;
import com.godaddy.vps4.notifications.NotificationType;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.VmResource;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.HS_AGENT, GDUser.Role.SUSPEND_AUTH})
public class CustomNotesResource {
    private static final Logger logger = LoggerFactory.getLogger(CustomNotesResource.class);

    private final CustomNotesService customNotesService;
    private final GDUser gdUser;
    private final VmResource vmResource;

    @Inject
    public CustomNotesResource(GDUser user, CustomNotesService customNotesService, VmResource vmResource) {
        this.vmResource = vmResource;
        this.customNotesService = customNotesService;
        this.gdUser = user;

    }

    @POST
    @Path("/{vmId}/customNote")
    public CustomNote createCustomNote(@PathParam("vmId") UUID vmId, CustomNoteRequest request) {
        vmResource.getVm(vmId);
        return customNotesService.createCustomNote(vmId, request.note, gdUser.getUsername());
    }

    @GET
    @Path("/{vmId}/customNotes")
    public List<CustomNote> getCustomNotes(@PathParam("vmId") UUID vmId) {
        vmResource.getVm(vmId);
        return customNotesService.getCustomNotes(vmId);
    }

    public static class CustomNoteRequest {
        public String note;
    }
}
