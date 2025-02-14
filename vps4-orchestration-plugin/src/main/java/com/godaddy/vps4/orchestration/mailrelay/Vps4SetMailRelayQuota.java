package com.godaddy.vps4.orchestration.mailrelay;

import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuota;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.json.simple.JSONObject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;


@CommandMetadata(
        name="Vps4SetMailRelayQuota",
        requestType=Vps4SetMailRelayQuota.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4SetMailRelayQuota extends ActionCommand<Vps4SetMailRelayQuota.Request, Void>{


    @Inject
    public Vps4SetMailRelayQuota(ActionService actionService) {
        super(actionService);
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request){
        SetMailRelayQuota.Request hfsRequest = new SetMailRelayQuota.Request();
        hfsRequest.ipAddress = request.ipAddress;
        hfsRequest.quota = request.mailRelayQuota;
        context.execute(SetMailRelayQuota.class, hfsRequest);
        return null;
    }

    public static class Request extends VmActionRequest{
        public int mailRelayQuota;
        public String ipAddress;

        // Empty constructor required for Jackson
        public Request(){}

        public Request(String ipAddress, int mailRelayQuota){
            this.ipAddress = ipAddress;
            this.mailRelayQuota = mailRelayQuota;
        }

        @SuppressWarnings("unchecked")
        public String toJSONString(){
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("IpAddress", ipAddress);
            jsonRequest.put("MailRelayQuota", mailRelayQuota);
            return jsonRequest.toJSONString();
        }
    }
}
