package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.RebuildVmRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.util.Cryptography;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class RebuildVm implements Command<RebuildVm.Request, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(RebuildVm.class);
    private final VmService vmService;
    private final Cryptography cryptography;

    @Inject
    public RebuildVm(VmService vmService, Cryptography cryptography) {
        this.vmService = vmService;
        this.cryptography = cryptography;
    }

    @Override
    public VmAction execute(CommandContext context, RebuildVm.Request request) {
        logger.info("sending HFS VM request: {}", request);

        RebuildVmRequest rebuildVmRequest = getRebuildVmRequest(request);

        VmAction vmAction = context.execute("RebuildVmHfs",
                ctx -> vmService.rebuildVm(request.vmId, rebuildVmRequest), VmAction.class);

        context.execute(WaitForVmAction.class, vmAction);

        return vmAction;
    }

    private RebuildVmRequest getRebuildVmRequest(Request request) {
        RebuildVmRequest rebuildVmRequest = new RebuildVmRequest();
        rebuildVmRequest.hostname = request.hostname;
        rebuildVmRequest.ignore_whitelist = request.ignore_whitelist;
        rebuildVmRequest.image_id = request.image_id;
        rebuildVmRequest.image_name = request.image_name;
        rebuildVmRequest.username = request.username;
        rebuildVmRequest.password = cryptography.decrypt(request.encryptedPassword);
        rebuildVmRequest.os = request.os;
        return rebuildVmRequest;
    }

    public static class Request {
        public long vmId;
        public String hostname;
        public String image_id;
        public String image_name;
        public String username;
        public byte[] encryptedPassword;
        public String os;
        public String ignore_whitelist;
    }
}
