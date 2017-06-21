package com.godaddy.vps4.web;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.security.jdbc.AuthorizationException;

@Provider
public class Vps4ExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ExceptionMapper.class);

    @Override
    public Response toResponse(Throwable t) {

        if (t instanceof WebApplicationException) {
            logger.debug("writing response for web exception", t);
            WebApplicationException wae = (WebApplicationException)t;
            return wae.getResponse();
        }

        if (t instanceof Vps4Exception) {
            Vps4Exception ve = (Vps4Exception)t;

            logger.warn("writing response for VPS4 exception", ve);

            JSONObject json = new JSONObject();
            json.put("id", ve.getId());

            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            if (ve.getId().equals("CONFLICTING_INCOMPLETE_ACTION") || ve.getId().equals("INVALID_STATUS"))
                status = Response.Status.CONFLICT;
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(json.toJSONString())
                    .build();
        }

        if (t instanceof AuthorizationException) {
            AuthorizationException ae = (AuthorizationException) t;

            logger.warn("writing response for Authorization exception", ae);

            JSONObject json = new JSONObject();
            json.put("id", "NOT_FOUND");

            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(json.toJSONString())
                    .build();
        }

        logger.warn("general exception", t);
        return Response.serverError()
                    .type(MediaType.APPLICATION_JSON)
                    .build();
    }

}
