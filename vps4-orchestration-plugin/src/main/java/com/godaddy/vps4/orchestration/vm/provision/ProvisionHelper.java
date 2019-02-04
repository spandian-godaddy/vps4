package com.godaddy.vps4.orchestration.vm.provision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.util.MonitoringMeta;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.CreateVmStep;
import com.godaddy.vps4.vm.Image;
import gdg.hfs.vhfs.nodeping.CheckType;
import gdg.hfs.vhfs.nodeping.CreateCheckRequest;
import gdg.hfs.vhfs.nodeping.NodePingLocation;

import java.util.Arrays;

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

        return createVmRequest;
    }

    public static SetPassword.Request createSetRootPasswordRequest(long hfsVmId, byte[] encryptedPassword) {
        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = hfsVmId;
        String[] usernames = { "root" };
        setPasswordRequest.usernames = Arrays.asList(usernames);
        setPasswordRequest.encryptedPassword = encryptedPassword;

        return setPasswordRequest;
    }

    public static ToggleAdmin.Request getToggleAdminRequest(ProvisionRequest request, long hfsVmId) {
        boolean adminEnabled = request.vmInfo.image.controlPanel == Image.ControlPanel.MYH;
        String username = request.username;
        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.enabled = adminEnabled;
        toggleAdminRequest.vmId = hfsVmId;
        toggleAdminRequest.username = username;

        return toggleAdminRequest;
    }

    public static CreateCheckRequest getCreateCheckRequest(String ipAddress, MonitoringMeta monitoringMeta) {
        CreateCheckRequest checkRequest = new CreateCheckRequest();
        checkRequest.target = ipAddress;
        checkRequest.label = ipAddress;
        // how often check is run in minutes
        checkRequest.interval = 1;
        // minutes delay to wait for recovery before alerting
        checkRequest.notificationDelay = 5;
        checkRequest.type = CheckType.PING;
        // geographical region where probe server should be located
        checkRequest.location = NodePingLocation.valueOf(monitoringMeta.getGeoRegion().toUpperCase());
        // kafka topic to consume for alerts
        checkRequest.notificationTopic = monitoringMeta.getNotificationTopic();

        return checkRequest;
    }


}
