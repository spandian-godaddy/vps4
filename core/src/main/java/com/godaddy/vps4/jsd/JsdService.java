package com.godaddy.vps4.jsd;

import com.godaddy.vps4.jsd.model.CreateJsdTicketRequest;
import com.godaddy.vps4.jsd.model.JsdCreatedComment;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;
import com.godaddy.vps4.jsd.model.JsdIssueSearchResult;

import java.time.Instant;
import java.util.UUID;

public interface JsdService {
    JsdCreatedIssue createTicket(CreateJsdTicketRequest createJsdTicketRequest);
    JsdIssueSearchResult searchTicket(String primaryIpAddress, Long outageId, UUID orionId);
    JsdCreatedComment commentTicket(String ticketIdOrKey, String fqdn, String items, Instant timestamp);
}
