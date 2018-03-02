package com.godaddy.vps4.console;

import gdg.hfs.vhfs.vm.Console;
import gdg.hfs.vhfs.vm.VmService;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

public class SpiceConsole implements ConsoleService {

    final VmService vmService;

    @Inject
    public SpiceConsole(VmService vmService){
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
}