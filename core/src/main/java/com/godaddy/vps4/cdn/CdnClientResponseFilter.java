package com.godaddy.vps4.cdn;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class CdnClientResponseFilter implements ClientResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(CdnClientResponseFilter.class);

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext)
            throws IOException {
        if (clientResponseContext.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientResponseContext.getEntityStream()));
            String errorMessage = reader.lines().collect(Collectors.joining());
            logger.info("Error response with status {} returned. Response body: {}.",
                        clientResponseContext.getStatus(), StringUtils.left(errorMessage, 1024));
        }
    }
}
