package com.godaddy.vps4.orchestration.vm;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel;
import com.godaddy.vps4.orchestration.hfs.cpanel.ConfigureCpanel.ConfigureCpanelRequest;
import com.godaddy.vps4.orchestration.hfs.cpanel.RefreshCpanelLicense;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp.BindIpRequest;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk.ConfigurePleskRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.RebuildVmInfo;
import com.godaddy.vps4.vm.RebuildVmStep;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CommandMetadata(
        name="Vps4RebuildVm",
        requestType=Vps4RebuildVm.Request.class,
        responseType=Vps4RebuildVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RebuildVm extends ActionCommand<Vps4RebuildVm.Request, Vps4RebuildVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RebuildVm.class);
    private final VmService vmService;
    private final VirtualMachineService virtualMachineService;
    private final NetworkService vps4NetworkService;
    private final VmUserService vmUserService;
    private final CreditService creditService;
    private Request request;
    private ActionState state;
    private CommandContext context;
    private UUID vps4VmId;

    @Inject
    public Vps4RebuildVm(ActionService actionService, VmService vmService, VirtualMachineService virtualMachineService,
                         NetworkService vps4NetworkService, VmUserService vmUserService, CreditService creditService) {
        super(actionService);
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.vps4NetworkService = vps4NetworkService;
        this.vmUserService = vmUserService;
        this.creditService = creditService;
    }

    @Override
    public Response executeWithAction(CommandContext context, Request request) throws Exception {
        this.request = request;
        this.context = context;
        this.state = new ActionState();
        this.vps4VmId = request.rebuildVmInfo.vmId;

        long oldHfsVmId = getOldHfsVmId();
        List<IpAddress> ipAddresses = getPublicIpAddresses();

        Vm hfsVm = createVm();
        long newHfsVmId = hfsVm.vmId;

        if(newHfsVmId == 0) {
            throw new  Exception("HFS Vm ID is not available. Expecting HFS VM ID.");
        }

        VirtualMachine oldVm = virtualMachineService.getVirtualMachine(vps4VmId);

        unbindPublicIpAddresses(ipAddresses);
        bindPublicIpAddress(newHfsVmId, ipAddresses);
        updateVmUser(request.rebuildVmInfo.username, oldVm.vmId, request.rebuildVmInfo.vmId);
        setRootUserPassword(newHfsVmId);
        configureControlPanel(newHfsVmId);
        setHostname(newHfsVmId);
        configureAdminUser(newHfsVmId);
        configureMailRelay(newHfsVmId);
        refreshCpanelLicense();

        updateServerDetails(request);
        setEcommCommonName(oldVm.orionGuid, request.rebuildVmInfo.serverName);

        // remove any support users on the old vm (or in the db)
        removeSupportUsers(vps4VmId);
        // delete the old vm
        deleteOldVm(oldHfsVmId);

        setStep(RebuildVmStep.RebuildComplete);

        logger.info("Completed VM Rebuild.");
        return null;
    }

    private void configureMailRelay(long hfsVmId) {
        setStep(RebuildVmStep.ConfigureMailRelay);

        String controlPanel = request.rebuildVmInfo.image.controlPanel.equals(Image.ControlPanel.MYH) ? null
                : request.rebuildVmInfo.image.controlPanel.name().toLowerCase();

        ConfigureMailRelayRequest configureMailRelayRequest = new ConfigureMailRelayRequest(hfsVmId, controlPanel);
        context.execute(ConfigureMailRelay.class, configureMailRelayRequest);

    }

    private void updateServerDetails(Request request) {
        // Update the servers name and the image.
        // Self managed customers can provision several different images
        String serverName = request.rebuildVmInfo.serverName;
        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("name", serverName);
        vmPatchMap.put("image_id", request.rebuildVmInfo.image.imageId);
        virtualMachineService.updateVirtualMachine(this.vps4VmId, vmPatchMap);
    }

    private long getOldHfsVmId() {
        return context.execute("GetHfsVmId", ctx -> virtualMachineService.getVirtualMachine(vps4VmId).hfsVmId, long.class);
    }

    private List<IpAddress> getPublicIpAddresses() {
        return vps4NetworkService.getVmIpAddresses(vps4VmId);
    }

    private void unbindPublicIpAddresses(List<IpAddress> ipAddresses) {
        setStep(RebuildVmStep.UnbindingIPAddress);
        for (IpAddress ipAddress: ipAddresses) {
            logger.info("Unbind public ip address {} from VM {}", ipAddress.ipAddress, vps4VmId);
            UnbindIp.Request unbindIpRequest = new UnbindIp.Request();
            unbindIpRequest.addressId = ipAddress.ipAddressId;
            unbindIpRequest.forceIfVmInaccessible = true;
            context.execute(String.format("UnbindIP-%d", ipAddress.ipAddressId), UnbindIp.class, unbindIpRequest);
        }
    }

    private CreateVm.Request createHfsVmRequest() {
        CreateVm.Request createVm = new CreateVm.Request();
        createVm.sgid = request.rebuildVmInfo.sgid;
        createVm.image_name = request.rebuildVmInfo.image.hfsName;
        createVm.rawFlavor = request.rebuildVmInfo.rawFlavor;
        createVm.username = request.rebuildVmInfo.username;
        createVm.zone = request.rebuildVmInfo.zone;
        createVm.encryptedPassword = request.rebuildVmInfo.encryptedPassword;
        createVm.hostname = request.rebuildVmInfo.hostname;
        return createVm;
    }

    private Vm createVm() {
        setStep(RebuildVmStep.RequestingServer);
        logger.info("create vm for rebuild process");

        CreateVm.Request createVmRequest = createHfsVmRequest();
        VmAction vmAction = context.execute("CreateVm", CreateVm.class, createVmRequest);

        // ensure the vm row is updated in the vps4 DB with the hfs vm id from the vm action.
        // having the hfs vm id in the vps4 database helps to track down the VM
        // in the event of a HFS VM provisioning failure.
        // Post creation reconfigure steps
        updateHfsVmId(vmAction.vmId);

        context.execute(WaitForVmAction.class, vmAction);

        // Get the hfs vm
        return context.execute("GetVmAfterCreate", ctx -> vmService.getVm(vmAction.vmId), Vm.class);
    }

    private void updateHfsVmId(long hfsVmId) {
        context.execute("UpdateHfsVmId", ctx -> {
            virtualMachineService.addHfsVmIdToVirtualMachine(vps4VmId, hfsVmId);
            return null;
        }, Void.class);
    }

    private void updateVmUser(String username, UUID oldVmId, UUID newVmId) {
        vmUserService.listUsers(oldVmId, VmUserType.CUSTOMER).stream()
                .forEach(user -> vmUserService.deleteUser(user.username, oldVmId));
        vmUserService.createUser(username, newVmId);
    }

    private void setRootUserPassword(long hfsVmId) {
        // set the root password to the same as the user password (LINUX ONLY)
        if(virtualMachineService.isLinux(vps4VmId)) {
            SetPassword.Request setRootPasswordRequest = createSetRootPasswordRequest(hfsVmId);
            context.execute("SetRootUserPassword", SetPassword.class, setRootPasswordRequest);
        }
    }

    private SetPassword.Request createSetRootPasswordRequest(long hfsVmId) {
        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = hfsVmId;
        String[] usernames = {"root"};
        setPasswordRequest.usernames = Arrays.asList(usernames);
        setPasswordRequest.encryptedPassword = request.rebuildVmInfo.encryptedPassword;
        return setPasswordRequest;
    }

    private void configureAdminUser(long hfsVmId) {
        boolean adminEnabled = !virtualMachineService.hasControlPanel(vps4VmId);
        String username = request.rebuildVmInfo.username;
        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.enabled = adminEnabled;
        toggleAdminRequest.vmId = hfsVmId;
        toggleAdminRequest.username = username;

        context.execute("ConfigureAdminAccess", ToggleAdmin.class, toggleAdminRequest);
        context.execute("UpdateVps4AdminUser", ctx -> {
            vmUserService.updateUserAdminAccess(username, vps4VmId, adminEnabled);
            return null;
        }, Void.class);
    }

    private void configureControlPanel(long hfsVmId) {
        Image image = virtualMachineService.getVirtualMachine(hfsVmId).image;
        if (image.controlPanel == Image.ControlPanel.CPANEL) {
            // VM with cPanel
            setStep(RebuildVmStep.ConfiguringCPanel);

            // configure cpanel on the vm
            ConfigureCpanelRequest cpanelRequest = createConfigureCpanelRequest(hfsVmId);
            context.execute(ConfigureCpanel.class, cpanelRequest);

        } else if (image.controlPanel == Image.ControlPanel.PLESK) {
            // VM with Plesk image
            setStep(RebuildVmStep.ConfiguringPlesk);

            // configure Plesk on the vm
            ConfigurePleskRequest pleskRequest = createConfigurePleskRequest(hfsVmId);
            context.execute(ConfigurePlesk.class, pleskRequest);
        }
    }

    private ConfigureCpanelRequest createConfigureCpanelRequest(long vmId) {
        ConfigureCpanelRequest cpanelRequest = new ConfigureCpanelRequest();
        cpanelRequest.vmId = vmId;
        return cpanelRequest;
    }

    private ConfigurePleskRequest createConfigurePleskRequest(long hfsVmId) {
        return new ConfigurePleskRequest(hfsVmId, request.rebuildVmInfo.username, request.rebuildVmInfo.encryptedPassword);
    }


    private void setHostname(long hfsVmId) {
        setStep(RebuildVmStep.SetHostname);

        SetHostname.Request hfsRequest = new SetHostname.Request(hfsVmId, request.rebuildVmInfo.hostname,
                request.rebuildVmInfo.image.controlPanel.toString());

        context.execute(SetHostname.class, hfsRequest);
    }

    private void setEcommCommonName(UUID orionGuid, String commonName) {
        context.execute("SetCommonName", ctx -> {
            creditService.setCommonName(orionGuid, commonName);
            return null;
        }, Void.class);
    }

    private void bindPublicIpAddress(long hfsVmId, List<IpAddress> ipAddresses) {
        setStep(RebuildVmStep.ConfiguringNetwork);
        logger.info("Bind public ip address");

        for (IpAddress ipAddress: ipAddresses) {
            logger.info("Bind public ip address {} to VM {}", ipAddress.ipAddress, vps4VmId);

            BindIpRequest bindRequest = new BindIpRequest();
            bindRequest.addressId = ipAddress.ipAddressId;
            bindRequest.vmId = hfsVmId;
            context.execute(String.format("BindIP-%d", ipAddress.ipAddressId), BindIp.class, bindRequest);
        }
    }

    private void refreshCpanelLicense(){
        VirtualMachine vm = context.execute("GetVirtualMachine",
                ctx -> virtualMachineService.getVirtualMachine(vps4VmId),
                VirtualMachine.class);
        if(vm.image.controlPanel.equals(Image.ControlPanel.CPANEL)){
            RefreshCpanelLicense.Request req = new RefreshCpanelLicense.Request();
            req.hfsVmId = vm.hfsVmId;
            context.execute("RefreshCPanelLicense", RefreshCpanelLicense.class, req);
        }
    }

    private void removeSupportUsers(UUID vps4VmId) {
        List<VmUser> users = vmUserService.listUsers(vps4VmId, VmUserType.SUPPORT);
        for(VmUser user : users) {
            if(user != null) {
                vmUserService.deleteUser(user.username, vps4VmId);
            }
        }
    }

    private void deleteOldVm(long hfsVmId) {
        setStep(RebuildVmStep.DeleteOldVm);
        logger.info("Delete old VM");
        context.execute("DestroyVmHfs", DestroyVm.class, hfsVmId);
    }

    protected void setStep(RebuildVmStep step) {
        state.step = step;

        try {
            actionService.updateActionState(request.getActionId(), mapper.writeValueAsString(state));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Request implements ActionRequest {
        public RebuildVmInfo rebuildVmInfo;
        public long actionId;

        @Override
        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }

    public static class Response {
        public long vmId;
    }

    public static class ActionState {
        public RebuildVmStep step;
    }
}
