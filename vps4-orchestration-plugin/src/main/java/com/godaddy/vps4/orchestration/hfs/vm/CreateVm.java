package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.Cryptography;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class CreateVm implements Command<CreateVm.Request, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(CreateVm.class);
    private final Cryptography cryptography;
    private final VmService vmService;

    @Inject
    public CreateVm(VmService vmService, Cryptography cryptography) {
        this.vmService = vmService;
        this.cryptography = cryptography;
    }

    @Override
    public VmAction execute(CommandContext context, CreateVm.Request request) {
        logger.info("sending HFS VM request: {}", request);

        CreateVMWithFlavorRequest hfsRequest = new CreateVMWithFlavorRequest();
        hfsRequest.sgid = request.sgid;
        hfsRequest.image_name = request.image_name;
        hfsRequest.rawFlavor = request.rawFlavor;
        hfsRequest.username = request.username;
        hfsRequest.zone = request.zone;
        hfsRequest.password = cryptography.decrypt(request.encryptedPassword);
        hfsRequest.hostname = request.hostname;

        VmAction vmAction = context.execute("CreateVmHfs", ctx -> vmService.createVmWithFlavor(hfsRequest));

        context.execute(WaitForVmAction.class, vmAction);

        return vmAction;
    }

    public static class Request {
        public String sgid;
        public String image_name;
        public String rawFlavor;
        public String username;
        public String zone;
        public byte[] encryptedPassword;
        public String hostname;
    }
}
