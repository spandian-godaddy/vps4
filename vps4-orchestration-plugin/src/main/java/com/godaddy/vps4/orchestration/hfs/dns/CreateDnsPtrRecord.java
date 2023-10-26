package com.godaddy.vps4.orchestration.hfs.dns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.dns.HfsDnsAction;
import com.godaddy.hfs.dns.HfsDnsService;
import com.godaddy.vps4.orchestration.dns.WaitForDnsAction;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class CreateDnsPtrRecord implements Command<CreateDnsPtrRecord.Request,Void> {

    private static final Logger logger = LoggerFactory.getLogger(CreateDnsPtrRecord.class);

    private final HfsDnsService hfsDnsService;

    @Inject
    public CreateDnsPtrRecord(HfsDnsService hfsDnsService){
        this.hfsDnsService = hfsDnsService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        logger.info("Requesting HFS to create reverse dns name {} for vm {}", request.reverseDnsName,
                request.virtualMachine.vmId);
        HfsDnsAction hfsDnsAction = context.execute("HfsCreateDnsPtrRecord",
                ctx -> hfsDnsService
                        .createDnsPtrRecord(request.virtualMachine.hfsVmId,
                                request.reverseDnsName),
                HfsDnsAction.class);
        context.execute(WaitForDnsAction.class, hfsDnsAction);
        return null;
    }

    public static class Request {
        public String reverseDnsName;
        public VirtualMachine virtualMachine;
    }
}
