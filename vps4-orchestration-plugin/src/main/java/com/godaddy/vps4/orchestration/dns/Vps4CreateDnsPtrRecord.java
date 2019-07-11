package com.godaddy.vps4.orchestration.dns;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.dns.CreateDnsPtrRecord;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4CreateDnsPtrRecord",
        requestType = Vps4CreateDnsPtrRecord.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4CreateDnsPtrRecord extends ActionCommand<Vps4CreateDnsPtrRecord.Request, Void> {

    @Inject
    public Vps4CreateDnsPtrRecord(ActionService actionService) {
        super(actionService);
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request) {
        // Wraps the CreateDnsPtrRecord command with a Vps4ActionCommand to handle
        // all of the action work needed for vps4.

        CreateDnsPtrRecord.Request hfsRequest = new CreateDnsPtrRecord.Request();
        hfsRequest.reverseDnsName = request.reverseDnsName;
        hfsRequest.virtualMachine = request.virtualMachine;
        context.execute(CreateDnsPtrRecord.class, hfsRequest);

        return null;
    }

    public static class Request extends VmActionRequest {
        public String reverseDnsName;
        public VirtualMachine virtualMachine;

        // Empty constructor required for Jackson
        public Request(){}

        public Request(String reverseDnsName, VirtualMachine virtualMachine){
            this.reverseDnsName = reverseDnsName;
            this.virtualMachine = virtualMachine;
        }

    }
}
