package com.godaddy.vps4.web;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;

@Provider
public class Vps4ExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ExceptionMapper.class);

    @Override
    public Response toResponse(Throwable t) {

        logger.debug("writing response for exception", t);

        if (t instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException)t;
            return wae.getResponse();
        }

        if (t instanceof Vps4Exception) {
            Vps4Exception ve = (Vps4Exception)t;
            JSONObject json = new JSONObject();
            json.put("id", ve.getId());

            return Response.serverError()
                    .type(MediaType.APPLICATION_JSON)
                    .entity(json.toJSONString())
                    .build();
        }

        return Response.serverError()
                    .type(MediaType.APPLICATION_JSON)
                    .build();
    }

}
