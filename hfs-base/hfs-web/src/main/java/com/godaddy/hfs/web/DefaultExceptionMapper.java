package com.godaddy.hfs.web;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionMapper.class);

    @Override
    public Response toResponse(Throwable t) {

        logger.debug("writing response for exception", t);

        if (t instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException)t;
            return wae.getResponse();
        }

        return Response.serverError()
                    .type(MediaType.APPLICATION_JSON)
                    .build();
    }

}
