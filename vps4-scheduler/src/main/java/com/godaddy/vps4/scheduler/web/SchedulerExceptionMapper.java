package com.godaddy.vps4.scheduler.web;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class SchedulerExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerExceptionMapper.class);

    @Override
    public Response toResponse(Throwable t) {
        logger.warn("Handling exception", t);

        if (t instanceof WebApplicationException) {
            return getWebExceptionResponse((WebApplicationException) t);
        }
        else if (t instanceof Vps4SchedulerException) {
            return getSchedulerExceptionResponse((Vps4SchedulerException) t);
        }
        else {
            return getGenericExceptionResponse(t);
        }
    }

    private Response getGenericExceptionResponse(Throwable t) {
        logger.warn("Generating response for general exception");
        return Response.serverError()
                    .type(MediaType.APPLICATION_JSON)
                    .build();
    }

    private Response getSchedulerExceptionResponse(Vps4SchedulerException ve) {
        logger.warn("Generating response for VPS4 scheduler exception");
        Response.Status status = getStatusForSchedulerException(ve.getId());
        return getJsonResponse(status, ve.getId());
    }

    private Response.Status getStatusForSchedulerException(String exceptionId) {
        Response.Status status;
        switch (exceptionId) {
            case "JOB_CREATION_ERROR":
                status = Response.Status.CONFLICT;
                break;
            case "NOT_FOUND":
                status = Response.Status.NOT_FOUND;
                break;
            case "JOB_DELETION_ERROR":
                status = Response.Status.BAD_REQUEST;
                break;
            default:
                status = Response.Status.BAD_REQUEST;
                break;
        }

        return status;
    }

    private Response getWebExceptionResponse(WebApplicationException ve) {
        logger.debug("Generating response for web exception");
        Response.Status status = Response.Status.fromStatusCode(ve.getResponse().getStatus());
        return getJsonResponse(status, status.name());
    }

    private Response getJsonResponse(Response.Status status, String responseId) {
        JSONObject json = new JSONObject();
        json.put("id", responseId);
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(json.toJSONString())
                .build();
    }

}
