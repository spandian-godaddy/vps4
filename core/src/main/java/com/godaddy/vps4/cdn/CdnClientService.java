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
import java.util.UUID;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface CdnClientService {
    @GET
    @Path("/customers/{customerId}/sites")
    List<CdnSite> getCdnSites(@PathParam("customerId") UUID customerId);

    @GET
    @Path("/customers/{customerId}/sites/{siteId}")
    CdnDetail getCdnSiteDetail(@PathParam("customerId") UUID customerId, @PathParam("siteId") String siteId);

    @GET
    @Path("/customers/{customerId}/sites/{siteId}/cache/{invalidationId}")
    CdnClientInvalidateStatusResponse getCdnInvalidateStatus(@PathParam("customerId") UUID customerId, @PathParam("siteId") String siteId, @PathParam("invalidationId") String invalidationId);

    @DELETE
    @Path("/customers/{customerId}/sites/{siteId}/cache")
    CdnClientInvalidateCacheResponse invalidateCdnCache(@PathParam("customerId") UUID customerId, @PathParam("siteId") String siteId);

    @POST
    @Path("/customers/{customerId}/sites/{siteId}/validations")
    void requestCdnValidation(@PathParam("customerId") UUID customerId, @PathParam("siteId") String siteId);

    @DELETE
    @Path("/customers/{customerId}/sites/{siteId}")
    CdnClientResponse deleteCdnSite(@PathParam("customerId") UUID customerId, @PathParam("siteId") String siteId);

    @POST
    @Path("/customers/{customerId}/sites")
    CdnClientCreateResponse createCdnSite(@PathParam("customerId") UUID customerId, CdnClientCreateRequest request);

    @PATCH
    @Path("/customers/{customerId}/sites/{siteId}")
    CdnClientResponse modifyCdnSite(@PathParam("customerId") UUID customerId, @PathParam("siteId") String siteId, CdnClientUpdateRequest updateCdnRequest);

}
