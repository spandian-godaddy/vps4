package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.ResizeRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class ResizeOHVm implements Command<ResizeOHVm.Request, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(ResizeOHVm.class);
    private final VmService hfsVmService;

    @Inject
    public ResizeOHVm (VmService vmService) {
        this.hfsVmService = vmService;
    }

    @Override
    public VmAction execute(CommandContext context, Request request) {
        ResizeRequest hfsRequest = new ResizeRequest();
        hfsRequest.rawFlavor = request.newSpecName;

        VmAction action = context.execute("ResizeOHVm", ctx -> hfsVmService.resize(request.hfsVmId, hfsRequest), VmAction.class);
        context.execute(WaitForVmAction.class, action);
        return action;
    }

    public static class Request {
        public long hfsVmId;
        public String newSpecName;

        public Request(){}

        public Request(long hfsVmId, String newSpecName){
            this.hfsVmId = hfsVmId;
            this.newSpecName = newSpecName;
        }
    }

}
