package com.godaddy.vps4.orchestration.hfs.dns;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.dns.HfsDnsAction;
import com.godaddy.hfs.dns.HfsDnsService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4CreateReverseDnsNameRecord",
        requestType = Vps4ReverseDnsNameRecordRequest.class,
        responseType = HfsDnsAction.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4CreateReverseDnsNameRecord extends
        ActionCommand<Vps4ReverseDnsNameRecordRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4CreateReverseDnsNameRecord.class);
    private final HfsDnsService dnsService;

    @Inject
    public Vps4CreateReverseDnsNameRecord(HfsDnsService dnsService, ActionService actionService) {
        super(actionService);
        this.dnsService = dnsService;
    }

    @Override
    public Void executeWithAction(CommandContext context, Vps4ReverseDnsNameRecordRequest request) {

        logger.info("Requesting HFS to create reverse dns name {} for vm {}", request.reverseDnsName,
                request.virtualMachine.vmId);
        HfsDnsAction hfsDnsAction = context.execute("CreateReverseDnsNameRecord",
                                                    ctx -> dnsService
                                                            .createReverseDnsNameRecord(request.virtualMachine.hfsVmId,
                                                                                        request.reverseDnsName),
                                                    HfsDnsAction.class);
        context.execute(WaitForDnsAction.class, hfsDnsAction);

        return null;
    }
}
