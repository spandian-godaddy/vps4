package com.godaddy.vps4.web.vm;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.User;
import com.godaddy.vps4.vm.UserService;
import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {
    final UserService userService;

    @Inject
    public UserResource(UserService userService){
        this.userService = userService;
        
    }
    
    @GET
    @Path("/{vmId}/users")
    public List<User> getUsers(@PathParam("vmId") long vmId){
        return userService.listUsers(vmId);
    }
}
