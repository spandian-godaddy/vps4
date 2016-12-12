package com.godaddy.vps4.cpanel;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cpanel.CpanelClient.CpanelServiceType;
import com.godaddy.vps4.network.NetworkService;

public class DefaultVps4CpanelService implements Vps4CpanelService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultVps4CpanelService.class);

    final CpanelAccessHashService accessHashService;
    final NetworkService networkService;

    final int timeoutVal;

    @Inject
    public DefaultVps4CpanelService(CpanelAccessHashService accessHashService, NetworkService networkService, Config conf) {
        this(accessHashService, networkService, Integer.parseInt(conf.get("vps4.callable.timeout", "10000")));
    }

    public DefaultVps4CpanelService(CpanelAccessHashService accessHashService, NetworkService networkService, int timeoutVal) {
        this.accessHashService = accessHashService;
        this.networkService = networkService;
        this.timeoutVal = timeoutVal;
    }

    private String getVmIp(long vmId) {
        return networkService.getVmPrimaryAddress(vmId).ipAddress;
    }

    private String getOriginatorIp() {
        return "172.19.46.185";
    }

    interface CpanelClientHandler<T> {
        T handle(CpanelClient client)
                throws CpanelAccessDeniedException, CpanelTimeoutException, IOException;
    }

    <T> T withAccessHash(long vmId, CpanelClientHandler<T> handler)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {

        Instant timeoutAt = Instant.now().plus(timeoutVal, ChronoUnit.MILLIS);

        Exception lastThrown = null;

        while (Instant.now().isBefore(timeoutAt)) {

            // TODO remove the hardcoded values for IP
            String fromIp = getOriginatorIp();
            String publicIp = getVmIp(vmId);

            String accessHash = accessHashService.getAccessHash(vmId, publicIp, fromIp, timeoutAt);
            if (accessHash == null) {
                // we couldn't get the access hash, so no point in even
                // trying to contact the VM

                // TODO throw this as CpanelAccessDeniedException?
                return null;
            }

            // TODO make sure we're still within timeoutAt to actually
            //      make the call to the VM

            CpanelClient cPanelClient = new CpanelClient(publicIp, accessHash);

            try {
                // need to configure read timeout in HTTP client
                return handler.handle(cPanelClient);

            } catch (CpanelAccessDeniedException e) {

                logger.warn("Access denied for cPanel VM {}, invalidating access hash", publicIp);

                // we weren't able to access the target VM, which may be due to an
                // access hash we thought was good, but has now been invalidated,
                // so invalidate the access hash so a new one will be attempted
                //cached.invalidate(fetchedAt);
                accessHashService.invalidAccessHash(vmId, accessHash);
                lastThrown = e;

            } catch (IOException e) {
                logger.warn("Unable communicating with VM " + vmId, e);
                // we timed out attempting to connect/read from the target VM
                // or we had some other transport-level issue
                lastThrown = e;
            }

        }
        // if we've run out of time communicating with the VM, but our last issue
        // was that we were having auth issues, bubble that exception back up to
        // the client, since that's the best description of the troubles we're having
        if (lastThrown != null && lastThrown instanceof CpanelAccessDeniedException) {
            throw (CpanelAccessDeniedException)lastThrown;
        }

        // any other issue is bubbled as a general timeout exception
        throw new CpanelTimeoutException("Timed out retrying an operation on VM " + vmId);
    }

    @Override
    public List<CPanelAccount> listCpanelAccounts(long vmId)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {

        return withAccessHash(vmId, cPanelClient -> {

            JSONParser parser = new JSONParser();

            String sitesJson = cPanelClient.listSites();
            logger.debug("sites JSON: {}", sitesJson);
            try {
                JSONObject jsonObject = (JSONObject) parser.parse(sitesJson);
                JSONObject data = (JSONObject) jsonObject.get("data");
                JSONArray accnts = (JSONArray) data.get("acct");

                List<CPanelAccount> domains = new ArrayList<>();
                for (Object object : accnts) {
                    JSONObject accnt = (JSONObject) object;
                    domains.add(new CPanelAccount((String) accnt.get("domain")));
                }

                return domains;
            } catch (ParseException e) {
                throw new IOException("Error parsing cPanel account list response", e);
            }
        });
    }

    @Override
    public CPanelSession createSession(long vmId, String username, CpanelServiceType serviceType)
            throws CpanelAccessDeniedException, CpanelTimeoutException, IOException {

        return withAccessHash(vmId, cPanelClient -> cPanelClient.createSession(username, serviceType));
    }

}
