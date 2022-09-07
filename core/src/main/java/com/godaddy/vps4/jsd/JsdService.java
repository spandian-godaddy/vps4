package com.godaddy.vps4.jsd;

import com.godaddy.vps4.jsd.model.CreateJsdTicketRequest;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;

public interface JsdService {
    JsdCreatedIssue createTicket(CreateJsdTicketRequest createJSDTicketRequest) throws Exception;
}
