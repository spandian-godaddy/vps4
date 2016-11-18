package com.godaddy.vps4.cpanel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.config.Config;
import com.godaddy.vps4.cpanel.CpanelClient.CpanelServiceType;

public class DefaultVps4CpanelService implements Vps4CpanelService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultVps4CpanelService.class);

    final CpanelAccessHashService accessHashService;

    final int timeoutVal;

    @Inject
    public DefaultVps4CpanelService(CpanelAccessHashService accessHashService, Config conf) {
        this(accessHashService, Integer.parseInt(conf.get("vps4.callable.timeout", "10000")));
    }

    public DefaultVps4CpanelService(CpanelAccessHashService accessHashService, int timeoutVal) {
        this.accessHashService = accessHashService;
        this.timeoutVal = timeoutVal;
    }

    private String getVmIp(long vmId) {
        return "50.62.9.38";
    }

    // TODO consolidate access hash logic into withAccessHash(String accessHash) lambda

    @Override
    public List<CPanelAccount> listCpanelAccounts(long vmId) {

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

                logger.error("Exception listing CPanel accounts", e);

                // we weren't able to access the target VM, which may be due to an
                // access hash we thought was good, but has now been invalidated,
                // so invalidate the access hash so a new one will be attempted
                //cached.invalidate(fetchedAt);
                accessHashService.invalidAccessHash(vmId, accessHash);
            }
        }
        return null; // FIXME throw TimeoutException
    }

    @Override
    public CPanelSession createSession(long vmId, String username, CpanelServiceType serviceType) {
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
                return new CpanelClient(getVmIp(vmId), accessHash).createSession(username, serviceType);

            } catch ( TimeoutException e) {
                // we timed out attempting to connect/read from the target VM

            } catch ( /*UnauthorizedAccess*/Exception e) {

                logger.error("Exception creating CPanel session", e);

                // we weren't able to access the target VM, which may be due to an
                // access hash we thought was good, but has now been invalidated,
                // so invalidate the access hash so a new one will be attempted
                //cached.invalidate(fetchedAt);
                accessHashService.invalidAccessHash(vmId, accessHash);
            }
        }
        return null; // FIXME throw TimeoutException
    }

}
