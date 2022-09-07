package com.godaddy.vps4.jsd;

import com.godaddy.vps4.jsd.model.JsdApiIssueRequest;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface JsdApiService {
    @POST
    @Path("/issue")
    JsdCreatedIssue createTicket(JsdApiIssueRequest jsdApiIssueRequest) throws Exception;
}
