package com.godaddy.vps4.web;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.Environment;

@Vps4Api

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {

    public static class ApplicationStatus {
        public String version = Version.CURRENT;
        public Environment environment = Environment.CURRENT;
    }

    @GET
    public ApplicationStatus getStatus() {
        ApplicationStatus status = new ApplicationStatus();
        return status;
    }

}
