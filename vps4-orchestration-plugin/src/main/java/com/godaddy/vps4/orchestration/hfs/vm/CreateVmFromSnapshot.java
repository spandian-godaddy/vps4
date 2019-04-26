package com.godaddy.vps4.orchestration.hfs.vm;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;
import com.godaddy.vps4.util.Cryptography;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.vm.CreateVMWithFlavorRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;

public class CreateVmFromSnapshot implements Command<CreateVmFromSnapshot.Request, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(CreateVmFromSnapshot.class);

    final VmService vmService;
    private final Cryptography cryptography;
    private final HfsVmTrackingRecordService hfsVmTrackingRecordService;

    @Inject
    public CreateVmFromSnapshot(VmService vmService,
                                Cryptography cryptography, 
                                HfsVmTrackingRecordService hfsVmTrackingRecordService) {
        this.cryptography = cryptography;
        this.vmService = vmService;
        this.hfsVmTrackingRecordService = hfsVmTrackingRecordService;
    }

    @Override
    public VmAction execute(CommandContext context, Request request) {

        logger.info("sending HFS VM request: {}", request);

        CreateVMWithFlavorRequest hfsRequest = createHfsRequest(request);

        VmAction vmAction = context.execute("CreateVmHfs", ctx -> vmService.createVmWithFlavor(hfsRequest), VmAction.class);

        hfsVmTrackingRecordService.create(vmAction.vmId, request.vmId, request.orionGuid);

        context.execute(WaitForAndRecordVmAction.class, vmAction);

        return vmAction;
    }

    private CreateVMWithFlavorRequest createHfsRequest(Request request) {
        CreateVMWithFlavorRequest hfsProvisionRequest = new CreateVMWithFlavorRequest();
        hfsProvisionRequest.rawFlavor = request.rawFlavor;
        hfsProvisionRequest.sgid = request.sgid;
        hfsProvisionRequest.image_id = request.image_id;
        hfsProvisionRequest.os = request.os;
        hfsProvisionRequest.username = request.username;
        hfsProvisionRequest.password = cryptography.decrypt(request.encryptedPassword);
        hfsProvisionRequest.zone = request.zone;
        hfsProvisionRequest.hostname = request.hostname;
        hfsProvisionRequest.ignore_whitelist = request.ignore_whitelist;
        hfsProvisionRequest.private_label_id = request.privateLabelId;
        return hfsProvisionRequest;
    }

    public static class Request {
        public String sgid;
        public String image_id;
        public String rawFlavor;
        public String username;
        public String zone;
        public String os;
        public String ignore_whitelist;
        public byte[] encryptedPassword;
        public String hostname;
        public String privateLabelId;
        public UUID vmId;
        public UUID orionGuid;
    }

}
