package com.godaddy.vps4.oh.backups;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.oh.OhResponse;
import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.oh.backups.models.OhBackupType;

@Path("/v2.1/backup")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OhApiBackupService {
    @GET
    @Path("/")
    OhResponse<List<OhBackup>> getBackups(@QueryParam("package_uuid") UUID packageId,
                                          @QueryParam("state") OhBackupState state);

    // The package_uuid is not required by the OH API, but passing it ensures that the backup corresponds with the
    // requested server. This is used to ensure users actually own the backups they access.
    @GET
    @Path("/")
    OhResponse<OhBackup> getBackupWithAuthValidation(@QueryParam("package_uuid") UUID packageId,
                                                     @QueryParam("uuid") UUID backupId);

    @GET
    @Path("/")
    OhResponse<OhBackup> getBackup(@QueryParam("uuid") UUID id);

    @POST
    @Path("/")
    OhResponse<OhBackup> createBackup(@QueryParam("package_uuid") UUID packageId,
                                      @QueryParam("type") OhBackupType type);

    @PUT
    @Path("/")
    void restoreBackup(@QueryParam("package_uuid") UUID packageId,
                       @FormParam("uuid") UUID backupId,
                       @FormParam("action") String action);

    @DELETE
    @Path("/")
    void deleteBackup(@QueryParam("uuid") UUID id);
}
