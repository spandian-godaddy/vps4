package com.godaddy.vps4.console;

import com.godaddy.hfs.config.Config;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

import com.godaddy.hfs.vm.Console;
import com.godaddy.hfs.vm.ConsoleRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HfsConsoleService implements ConsoleService {
    private static final Logger logger = LoggerFactory.getLogger(HfsConsoleService.class);

    final Config config;
    final VmService vmService;

    @Inject
    public HfsConsoleService(Config config, VmService vmService){
        this.config = config;
        this.vmService = vmService;
    }

    public String getConsoleUrl(long hfsVmId) throws CouldNotRetrieveConsoleException{
        Console console = vmService.getConsole(hfsVmId);
        if(console == null ) {
            throw new CouldNotRetrieveConsoleException("Null console object returned for hfs vm " + hfsVmId);
        }
        else if (StringUtils.isBlank(console.url)) {
            throw new CouldNotRetrieveConsoleException("Empty console url returned for hfs vm " + hfsVmId);
        }
        return console.url;
    }

    public String getConsoleUrl(long hfsVmId, String allowedIpAddress) throws CouldNotRetrieveConsoleException{
        createConsoleUrl(hfsVmId, allowedIpAddress);
        return getConsoleUrl(hfsVmId);
    }

    private void createConsoleUrl(long hfsVmId, String allowedAddress) {
        ConsoleRequest request = new ConsoleRequest();
        request.allowedAddress = allowedAddress;
        VmAction action = vmService.createConsoleUrl(hfsVmId, request);
        waitForCreateConsoleAction(action);
    }

    private void waitForCreateConsoleAction(VmAction hfsAction){
        logger.info("waiting on createConsoleUrl: {}", hfsAction);
        int timeout = 60;
        while (isActionInProgress(hfsAction) && timeout-- > 0) {
            sleepOneSecond();
            hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);
        }

        if (!hfsAction.state.equals(VmAction.Status.COMPLETE)) {
            logger.warn("failed to generate console url {}", hfsAction);
            throw new RuntimeException("Generate Console URL Failed");
        }
    }

    private boolean isActionInProgress(VmAction hfsAction){
        return !hfsAction.state.equals(VmAction.Status.COMPLETE)
                && !hfsAction.state.equals(VmAction.Status.ERROR);
    }

    void sleepOneSecond(){
        try{
            Thread.sleep(1000);
        }catch (InterruptedException e) {
            logger.info("wait for createConsoleUrl action interrupted");
        }
    }

}
