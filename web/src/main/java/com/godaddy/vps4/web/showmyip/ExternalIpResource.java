package com.godaddy.vps4.web.showmyip;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "utililty" })

@Path("/api/utility")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExternalIpResource {

    private static final Logger logger = LoggerFactory.getLogger(ExternalIpResource.class);

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    
    @GET
    @Path("showmyip")
    public ExternalIp getClientIpFromRequestHeader(@Context HttpHeaders headers, @Context HttpServletRequest req) {

        // TODO: remove - only here for debug purposes.
        MultivaluedMap<String, String> allheadersMap = headers.getRequestHeaders();
        Iterator<String> i = allheadersMap.keySet().iterator();
        while(i.hasNext()) {
            String key = i.next();
            logger.info(" {} : {} ", key, allheadersMap.get(key));
        }
        
        List<String> xForwardedFor = headers.getRequestHeader(X_FORWARDED_FOR);
        if(xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return new ExternalIp(xForwardedFor.get(0).split(",")[0]);
        } else if(StringUtils.isNotBlank(req.getRemoteAddr())){
            return new ExternalIp(req.getRemoteAddr());
        }

        throw new NotAcceptableException("Unable to determine client IP address from request header. ");
    }
}
