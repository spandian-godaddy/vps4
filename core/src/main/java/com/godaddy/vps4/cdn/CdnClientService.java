package com.godaddy.vps4.cdn;


import com.godaddy.vps4.PATCH;
import com.godaddy.vps4.cdn.model.CdnClientCreateRequest;
import com.godaddy.vps4.cdn.model.CdnClientCreateResponse;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateCacheResponse;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateStatusResponse;
import com.godaddy.vps4.cdn.model.CdnClientResponse;
import com.godaddy.vps4.cdn.model.CdnClientUpdateRequest;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnSite;

import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;

import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;

import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface CdnClientService {
    @GET
    @Path("/sites")
    List<CdnSite> getCdnSites(@HeaderParam("Authorization") String ssoJwt);

    @GET
    @Path("/sites/{siteId}")
    CdnDetail getCdnSiteDetail(@HeaderParam("Authorization") String ssoJwt, @PathParam("siteId") String siteId);

    @GET
    @Path("/sites/{siteId}/cache/{invalidationId}")
    CdnClientInvalidateStatusResponse getCdnInvalidateStatus(@HeaderParam("Authorization") String ssoJwt, @PathParam("siteId") String siteId, @PathParam("invalidationId") String invalidationId);

    @POST
    @Path("/sites")
    CdnClientCreateResponse createCdnSite(@HeaderParam("Authorization") String ssoJwt, CdnClientCreateRequest request);

    @DELETE
    @Path("/sites/{siteId}/cache")
    CdnClientInvalidateCacheResponse invalidateCdnCache(@HeaderParam("Authorization") String ssoJwt, @PathParam("siteId") String siteId);

    @DELETE
    @Path("/sites/{siteId}")
    CdnClientResponse deleteCdnSite(@HeaderParam("Authorization") String ssoJwt, @PathParam("siteId") String siteId);

    @PATCH
    @Path("/sites/{siteId}")
    CdnClientResponse modifyCdnSite(@HeaderParam("Authorization") String ssoJwt, @PathParam("siteId") String siteId, CdnClientUpdateRequest updateCdnRequest);


}
