package com.godaddy.vps4.monitoring.iris;

import javax.inject.Inject;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.monitoring.MonitoringNotificationService;
import com.godaddy.vps4.monitoring.iris.irisClient.CreateIncidentInput;
import com.godaddy.vps4.monitoring.iris.irisClient.IrisWebServiceSoap;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

public class IrisMonitoringNotificationService implements MonitoringNotificationService {

    private final int SUBSCRIBER_ID = 232; // managed services
    private final int SERVICE_ID = 294; // Managed Services
    private final int CATAGORY_ID = 1381; // Nodeping
    private final int PRIORITY_ID = 5; // High Priority
    private final String CREATED_BY = "VPS4 Server Monitoring";

    private final IrisWebServiceSoap irisWebServiceSoap;
    private final int groupId;
    private final VirtualMachineService virtualMachineService;
    private final Vps4UserService vps4UserService;

    private String DUMMY_EMAIL = "alert@nodeping.com";

    @Inject
    IrisMonitoringNotificationService(Config config, VirtualMachineService virtualMachineService,
            Vps4UserService vps4UserService, IrisWebServiceSoap irisWebServiceSoap) {
        this.irisWebServiceSoap = irisWebServiceSoap;
        groupId = Integer.parseInt(config.get("monitoring.iris.group.id"));
        this.virtualMachineService = virtualMachineService;
        this.vps4UserService = vps4UserService;
    }

    @Override
    public long sendServerDownEventNotification(VirtualMachine vm) {
        long userId = virtualMachineService.getUserIdByVmId(vm.vmId);
        Vps4User user = vps4UserService.getUser(userId);

        CreateIncidentInput input = new CreateIncidentInput();
        input.setSubscriberId(SUBSCRIBER_ID);
        input.setServiceId(SERVICE_ID);
        input.setGroupId(groupId);
        input.setCategoryId(CATAGORY_ID);
        input.setPriorityId(PRIORITY_ID);
        input.setCreatedBy(CREATED_BY);
        input.setSubject(String.format("Host %s Down : PING %s", vm.hostname, vm.primaryIpAddress.ipAddress));
        input.setShopperId(user.getShopperId());
        //IRIS has trouble if you don't set an email address.
        //we don't have the customer email address so we were asked by managed services to use this address to be consistent with other nodeping events.
        input.setCustomerEmailAddress(DUMMY_EMAIL);

        StringBuilder note = new StringBuilder();
        note.append("Server Down event received\n");
        note.append("Hostname: " + vm.hostname + "\n");
        note.append("IP Address: " + vm.primaryIpAddress.ipAddress + "\n");
        note.append("Shopper ID: " + user.getShopperId() + "\n");
        note.append("Server ID: " + vm.vmId + "\n");
        note.append("Orion GUID: " + vm.orionGuid + "\n");
        note.append("HFS Server ID: " + vm.hfsVmId + "\n");
        note.append("Dashboard: https://myh.godaddy.com/#/hosting/vps4/servers/" + vm.orionGuid + "\n");
        note.append("*** You will need to impersonate the user before using this link. ***");
        input.setNote(note.toString());

        return irisWebServiceSoap.createIrisIncident(input);
    }

}
