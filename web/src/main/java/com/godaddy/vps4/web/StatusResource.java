package com.godaddy.vps4.web;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Vps4Api

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {

    public static class ApplicationStatus {
        public String version = Version.CURRENT;
    }

    @GET
    public ApplicationStatus getStatus() {
        ApplicationStatus status = new ApplicationStatus();
        return status;
    }

}
