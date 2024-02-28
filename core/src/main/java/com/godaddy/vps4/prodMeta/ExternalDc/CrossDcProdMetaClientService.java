package com.godaddy.vps4.prodMeta.ExternalDc;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.PATCH;
import com.godaddy.vps4.prodMeta.model.ProdMeta;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface CrossDcProdMetaClientService {

    @GET
    @Path("/{entitlementId}/prodMeta")
    ProdMeta getProdMeta(@PathParam("entitlementId") UUID orionGuid);

    @POST
    @Path("/{entitlementId}/prodMeta")
    ProdMeta setProdMeta(@PathParam("entitlementId") UUID orionGuid);

    @PATCH
    @Path("/{entitlementId}/prodMeta")
    ProdMeta updateProdMeta(@PathParam("entitlementId") UUID orionGuid, ProdMeta prodMeta);

    @DELETE
    @Path("/{entitlementId}/prodMeta")
    void deleteProdMeta(@PathParam("entitlementId") UUID orionGuid);
}
