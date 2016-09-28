package com.godaddy.vps4.web.validator;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.util.validators.Validator;
import com.godaddy.vps4.util.validators.ValidatorRegistry;
import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "validator" })

@Path("/validator")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ValidatorResource {

    @GET
    @Path("{field}")
    public Validator getValidationRules(@PathParam("field") String field) {
        return getValidator(field);
    }

    Validator getValidator(String field) {
        Validator validator = ValidatorRegistry.getInstance().get(field);
        if (validator == null) {
            throw new NotFoundException("Unknown field name: " + field);
        }
        return validator;
    }

}
