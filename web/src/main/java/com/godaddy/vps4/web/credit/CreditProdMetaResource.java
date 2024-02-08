package com.godaddy.vps4.web.credit;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.PATCH;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.prodMeta.ProdMetaService;
import com.godaddy.vps4.prodMeta.model.ProdMeta;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.google.inject.Inject;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "credits" })

@Path("/api/credits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = { GDUser.Role.ADMIN })
public class CreditProdMetaResource {
    private final ProdMetaService prodMetaService;
    private final CreditService creditService;
    private final DataCenterService dataCenterService;

    @Inject
    public CreditProdMetaResource(ProdMetaService prodMetaService, CreditService creditService, DataCenterService dataCenterService) {
        this.prodMetaService = prodMetaService;
        this.creditService = creditService;
        this.dataCenterService = dataCenterService;
    }

    @GET
    @Path("/{orionGuid}/prodMeta")
    public ProdMeta getProdMeta(@PathParam("orionGuid") UUID entitlementId) {
        ProdMeta prodMeta = prodMetaService.getProdMeta(entitlementId);
        if (prodMeta == null) {
            prodMeta = new ProdMeta(creditService.getProductMeta(entitlementId), dataCenterService);
            updateProdMeta(entitlementId, prodMeta);
        }
        return prodMeta;
    }

    @POST
    @Path("/{orionGuid}/prodMeta")
    public ProdMeta setProdMeta(@PathParam("orionGuid") UUID entitlementId) {
        prodMetaService.insertProdMeta(entitlementId);
        return prodMetaService.getProdMeta(entitlementId);
    }

    @PATCH
    @Path("/{orionGuid}/prodMeta")
    public ProdMeta updateProdMeta(@PathParam("orionGuid") UUID entitlementId, ProdMeta prodMeta) {
        ProdMeta currentProdMeta = prodMetaService.getProdMeta(entitlementId);
        if (currentProdMeta == null) {
            setProdMeta(entitlementId);
        }
        prodMetaService.updateProdMeta(entitlementId, prodMeta.getProdMetaMap());
        return prodMetaService.getProdMeta(entitlementId);
    }

    @DELETE
    @Path("/{orionGuid}/prodMeta")
    public void deleteProdMeta(@PathParam("orionGuid") UUID entitlementId) {
        prodMetaService.deleteProdMeta(entitlementId);
    }
}