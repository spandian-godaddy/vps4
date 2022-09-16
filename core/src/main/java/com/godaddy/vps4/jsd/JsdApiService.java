package com.godaddy.vps4.jsd;

import com.godaddy.vps4.jsd.model.JsdApiIssueCommentRequest;
import com.godaddy.vps4.jsd.model.JsdApiIssueRequest;
import com.godaddy.vps4.jsd.model.JsdApiSearchIssueRequest;
import com.godaddy.vps4.jsd.model.JsdCreatedComment;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;
import com.godaddy.vps4.jsd.model.JsdIssueSearchResult;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface JsdApiService {
    @POST
    @Path("/issue")
    JsdCreatedIssue createTicket(JsdApiIssueRequest jsdApiIssueRequest);

    @POST
    @Path("/issue/{issueIdOrKey}/comment")
    JsdCreatedComment commentTicket(@PathParam("issueIdOrKey") String issueIdOrKey, JsdApiIssueCommentRequest jsdApiIssueCommentRequest);

    @POST
    @Path("/search")
    JsdIssueSearchResult searchTicket(JsdApiSearchIssueRequest jsdApiSearchIssueRequest);
}
