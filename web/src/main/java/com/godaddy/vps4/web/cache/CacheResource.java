package com.godaddy.vps4.web.cache;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "cache" })

@Path("/api/cache")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.TEXT_PLAIN)
public class CacheResource {

    final Cache<String, String> cache;

    @Inject
    public CacheResource(CacheManager cache) {
        this.cache = cache.getCache("test", String.class, String.class);
    }

    @GET
    @Path("key/{key}")
    public String get(@PathParam("key") String key) {
        String value = cache.get(key);
        System.out.println("value: '" + value + "'");
        return value;
    }

    @POST
    @Path("key/{key}")
    public void set(@PathParam("key") String key, String value) {
        System.out.println("set " + key + " to value: " + value);
        cache.put(key, value);
    }

}
