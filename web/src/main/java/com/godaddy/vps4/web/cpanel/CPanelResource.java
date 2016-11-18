package com.godaddy.vps4.web.cpanel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.config.Config;
import com.godaddy.vps4.web.Action;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.cpanel.CpanelClient.CpanelService;

import gdg.hfs.vhfs.cpanel.CPanelService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CPanelResource {

    private static final Logger logger = LoggerFactory.getLogger(CPanelResource.class);

    static final Map<Long, Action> actions = new ConcurrentHashMap<>();

    static final AtomicLong actionIdPool = new AtomicLong();

    final CpanelAccessHashService accessHashService;

    final CPanelService controlPanelService;

    final Config conf;

    @Inject
    public CPanelResource(
            CpanelAccessHashService accessHashService,
            CPanelService controlPanelService,
            Config configProvider) {
        this.accessHashService = accessHashService;
        this.controlPanelService = controlPanelService;
        this.conf = configProvider;
    }

    @GET
    @Path("{vmId}/cpanel/session")
    public CPanelSession getCPanelSession(@PathParam("vmId") long vmId) {
        // TODO Get the VM instead of using hardcoded IP.
        try {
            return new CpanelClient(getVmIp(vmId), "ACCESS_HASH_GOES_HERE").createSession("root",
                    CpanelService.whostmgrd);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private String getVmIp(long vmId) {
        return "50.62.9.38";
    }

    @GET
    @Path("/{vmId}/cpanel/accounts")
    public List<CPanelAccount> listCpanelAccounts(@PathParam("vmId") long vmId)
            throws InterruptedException, ExecutionException, TimeoutException {

        logger.info("GET listCpanelAccounts for VM: {}", vmId);

        int timeoutVal = Integer.parseInt(conf.get("vps4.callable.timeout", "10000"));

        Instant timeoutAt = Instant.now().plus(timeoutVal, ChronoUnit.MILLIS);

        while (Instant.now().isBefore(timeoutAt)) {

            // TODO remove the hardcoded values for IP
            String accessHash = accessHashService.getAccessHash(vmId, "10.32.155.80", "172.19.46.185", timeoutAt);
            if (accessHash == null) {
                // we couldn't get the access hash, so no point in even
                // trying to contact the VM
                return null;
            }

            // TODO make sure we're still within timeoutAt to actually
            //      make the call to the VM

            try {
                // need to configure read timeout in HTTP client
                return new GetCpanelAccounts(vmId, getVmIp(vmId), accessHash).call();

            } catch ( TimeoutException e) {
                // we timed out attempting to connect/read from the target VM

            } catch ( /*UnauthorizedAccess*/Exception e) {

                // we weren't able to access the target VM, which may be due to an
                // access hash we thought was good, but has now been invalidated,
                // so invalidate the access hash so a new one will be attempted
                //cached.invalidate(fetchedAt);
                accessHashService.invalidAccessHash(vmId, accessHash);
            }
        }

        return null;


    }
}
