package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.RebuildDedicatedRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.util.Cryptography;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class RebuildDedicated implements Command<RebuildDedicated.Request, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(RebuildDedicated.class);
    private final VmService vmService;
    private final Cryptography cryptography;

    @Inject
    public RebuildDedicated(VmService vmService, Cryptography cryptography) {
        this.vmService = vmService;
        this.cryptography = cryptography;
    }

    @Override
    public VmAction execute(CommandContext context, RebuildDedicated.Request request) {
        logger.info("sending HFS VM request: {}", request);

        RebuildDedicatedRequest rebuildDedicatedRequest = getRebuildDedicatedRequest(request);

        VmAction vmAction = context.execute("RebuildDedicated",
                ctx -> vmService.rebuildVm(request.vmId, rebuildDedicatedRequest), VmAction.class);

        context.execute(WaitForVmAction.class, vmAction);

        return vmAction;
    }

    private RebuildDedicatedRequest getRebuildDedicatedRequest(Request request) {
        RebuildDedicatedRequest rebuildDedicatedRequest = new RebuildDedicatedRequest();
        rebuildDedicatedRequest.hostname = request.hostname;
        rebuildDedicatedRequest.ignore_whitelist = request.ignore_whitelist;
        rebuildDedicatedRequest.image_id = request.image_id;
        rebuildDedicatedRequest.image_name = request.image_name;
        rebuildDedicatedRequest.username = request.username;
        rebuildDedicatedRequest.password = cryptography.decrypt(request.encryptedPassword);
        rebuildDedicatedRequest.os = request.os;
        return rebuildDedicatedRequest;
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
