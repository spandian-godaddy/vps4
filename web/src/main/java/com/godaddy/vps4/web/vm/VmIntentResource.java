package com.godaddy.vps4.web.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Inject;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmIntentResource {

    @Inject
    public VmIntentResource() {        
    }

    @GET
    @Path("/intents")
    public List<Intent> getVmIntentOptions() {
        String[] intentOptions = new String[] {"Website Hosting", 
                             "Application Development and Testing", 
                             "Game Server", 
                             "File Storage and Backup", 
                             "Remote Desktop Access", 
                             "Email Server", 
                             "Data Analytics", 
                             "Voice over IP (VoIP) Server", 
                             "Proxy Server", 
                             "Other" };

        List<Intent> intents = new ArrayList<>();
        for (int i = 0; i < intentOptions.length; i++) {
            Intent intent = new Intent();
            intent.id = i;
            intent.name = intentOptions[i];
            intents.add(intent);
        }
        return intents;
    }

    @GET
    @Path("/{vmId}/intents")
    public List<Intent> getVmIntents(UUID vmId) {
        List<Intent> intents = getVmIntentOptions();
        return intents.subList(0, 2);    
    }

    @POST
    @Path("/{vmId}/intents")
    public List<Intent> setVmIntents(UUID vmId, List<Integer> intentIds, String otherIntentDescription) {
        List<Intent> intents = getVmIntentOptions();
        return intents.subList(0, 2);   
    }
}
