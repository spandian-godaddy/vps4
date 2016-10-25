package com.godaddy.vps4.web;

import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;

class Test {
    public String test;
}

@Vps4Api
@Api(tags = { "test" })

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestResource {

    static final AtomicInteger idPool = new AtomicInteger();

    @GET
    @Path("/")
    public Test getTest() {

        try {
            Thread.sleep(2000);
        } catch (Exception e) {

        }

        Test test = new Test();
        test.test = "test " + idPool.incrementAndGet();
        return test;
    }
}
