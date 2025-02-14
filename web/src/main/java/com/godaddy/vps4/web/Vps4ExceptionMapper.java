package com.godaddy.vps4.web;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class Vps4ExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ExceptionMapper.class);

    @Override
    public Response toResponse(Throwable t) {

        if (t instanceof WebApplicationException) {
            logger.debug("writing response for web exception", t);
            Response.Status status = Response.Status.fromStatusCode(
                    ((WebApplicationException) t).getResponse().getStatus());


            JSONObject json = new JSONObject();
            json.put("id", status.name());
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(json.toJSONString())
                    .build();
        }

        if (t instanceof Vps4Exception) {
            Vps4Exception ve = (Vps4Exception)t;

            if (ve.getId().equals("USAGE_STATS_UNAVAILABLE")) {
                logger.warn("writing response for VPS4 exception " + ve.getId());
            } else {
                logger.warn("writing response for VPS4 exception", ve);
            }

            JSONObject json = new JSONObject();
            json.put("id", ve.getId());
            json.put("message", ve.getMessage());

            Response.Status status = Response.Status.BAD_REQUEST;
            if (ve.getId().equals("CONFLICTING_INCOMPLETE_ACTION")
                    || ve.getId().equals("INVALID_STATUS")
                    || ve.getId().equals("INVALID_SNAPSHOT_NAME")
                    || ve.getId().equals("SNAPSHOT_OVER_QUOTA")
                    || ve.getId().equals("ALREADY_EXISTS")
                    || ve.getId().equals("SNAPSHOT_HV_LIMIT_REACHED")
                    || ve.getId().equals("SNAPSHOT_DC_LIMIT_REACHED"))
                status = Response.Status.CONFLICT;
            return Response.status(status)
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
