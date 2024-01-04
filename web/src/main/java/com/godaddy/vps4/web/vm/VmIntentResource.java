package com.godaddy.vps4.web.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.intent.IntentService;
import com.godaddy.vps4.intent.IntentUtils;
import com.godaddy.vps4.intent.model.Intent;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.google.inject.Inject;

import io.swagger.annotations.Api;


@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmIntentResource {
    private final IntentService intentService;
    private final VmResource vmResource;
    List<Intent> intents;
    Map<Integer, Intent> intentOptions;

    @Inject
    public VmIntentResource(IntentService intentService, VmResource vmResource) {
        this.intentService = intentService;        
        this.vmResource = vmResource;   
        intents = intentService.getIntents();
        intentOptions = IntentUtils.getIntentsMap(intents);
    }

    @GET
    @Path("/intents")
    public List<Intent> getVmIntentOptions() {
        return intents;
    }

    @GET
    @Path("/{vmId}/intents")
    public List<Intent> getVmIntents(@PathParam("vmId") UUID vmId) {
        vmResource.getVm(vmId); // auth validation
        return intentService.getVmIntents(vmId);
    }

    @POST
    @Path("/{vmId}/intents")
    public List<Intent> setVmIntents(@PathParam("vmId") UUID vmId, List<Integer> intentIds, String otherIntentDescription) {
        vmResource.getVm(vmId); // auth validation
        List<Intent> vmIntents = new ArrayList<>();

        for (Integer intentId : intentIds) {
            Intent intent = intentOptions.get(intentId);
            if (intent != null) {
                vmIntents.add(new Intent(intent));
                if(intent.name.equals("OTHER")) {
                    intent.description = otherIntentDescription;
                }
            }
            else {
                throw new Vps4Exception("INVALID_INTENT_ID", "ID " + intentId + " is not a valid intent ID");
            }
        }
        return intentService.setVmIntents(vmId, vmIntents);
    }
}
