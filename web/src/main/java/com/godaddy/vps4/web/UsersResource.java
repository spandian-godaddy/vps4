package com.godaddy.vps4.web;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.security.User;

import io.swagger.annotations.Api;

@Api(tags = { "users" })

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UsersResource {

    private static final Logger logger = LoggerFactory.getLogger(UsersResource.class);

    final User user;

    @Inject
    public UsersResource(DataSource dataSource, User user) {
        this.user = user;
    }

    @GET
    public User getUser() {

        logger.info("getting user");

        return user;
    }
}
