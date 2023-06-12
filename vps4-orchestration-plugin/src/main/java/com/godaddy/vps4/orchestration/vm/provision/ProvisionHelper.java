package com.godaddy.vps4.orchestration.vm.provision;

import java.util.Arrays;

import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.vm.CreateVmStep;

public class ProvisionHelper {

    public static class Response {
        public long vmId;
    }

    public static class ActionState {
        public CreateVmStep step;
    }

    public static CreateVm.Request getCreateVmRequest(ProvisionRequest request, String hostname) {
        CreateVm.Request createVmRequest = new CreateVm.Request();
        createVmRequest.hostname = hostname;
        createVmRequest.image_name = request.image_name;
        createVmRequest.rawFlavor = request.rawFlavor;
        createVmRequest.sgid = request.sgid;
        createVmRequest.username = request.username;
        createVmRequest.zone = request.zone;
        createVmRequest.encryptedPassword = request.encryptedPassword;
        createVmRequest.privateLabelId = request.privateLabelId;
        createVmRequest.vmId = request.vmInfo.vmId;
        createVmRequest.orionGuid = request.orionGuid;

        return createVmRequest;
    }

    public static SetPassword.Request createSetRootPasswordRequest(
        long hfsVmId, byte[] encryptedPassword, String controlPanel) {
        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = hfsVmId;
        String[] usernames = { "root" };
        setPasswordRequest.usernames = Arrays.asList(usernames);
        setPasswordRequest.encryptedPassword = encryptedPassword;
        setPasswordRequest.controlPanel = controlPanel;

        return setPasswordRequest;
    }

    public static ToggleAdmin.Request getToggleAdminRequest(ProvisionRequest request, long hfsVmId) {
        boolean adminEnabled = !request.vmInfo.image.hasPaidControlPanel();
        String username = request.username;
        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.enabled = adminEnabled;
        toggleAdminRequest.vmId = hfsVmId;
        toggleAdminRequest.username = username;

        return toggleAdminRequest;
    }

}
