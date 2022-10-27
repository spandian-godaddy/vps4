package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.security.AutoCreateVps4UserModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import javax.inject.Inject;
import java.util.Random;
import java.util.UUID;

public class CreateTestVmRequest {

    final VirtualMachineService virtualMachineService;

    final CreditService creditService;

    final ProjectService projectService;

    final ImageService imageService;

    final Vps4UserService userService;

    @Inject
    public CreateTestVmRequest(
            VirtualMachineService virtualMachineService,
            CreditService creditService,
            ProjectService projectService,
            ImageService imageService,
            Vps4UserService userService) {
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.projectService = projectService;
        this.imageService = imageService;
        this.userService = userService;
    }

    protected void provision(int tier, int managedLevel, int monitoring, String operatingSystem, String controlPanel, String shopperId, int resellerId, UUID customerId) {

        UUID orionGuid = UUID.randomUUID();
        creditService.createVirtualMachineCredit(orionGuid, shopperId, operatingSystem, controlPanel, tier, managedLevel, monitoring, resellerId, customerId);

        // normally we would get this from HFS
        long hfsVmId = new Random().nextInt(1000000);

        Vps4User user = userService.getOrCreateUserForShopper(shopperId, "1", UUID.randomUUID());

        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(user.getId(), 1, "vps4-testing-", orionGuid,
                "SomeNewVm", 1, 1, operatingSystem);
        UUID vmId = virtualMachineService.provisionVirtualMachine(params).vmId;
        virtualMachineService.addHfsVmIdToVirtualMachine(vmId, hfsVmId);
    }

    public static void main(String[] args) {

        final String shopperId = "5y5";

        Injector injector = Guice.createInjector(
                new ConfigModule(), new DatabaseModule(),
                new VmModule(), new SecurityModule(), new AutoCreateVps4UserModule(shopperId));

        CreateTestVmRequest force = injector.getInstance(CreateTestVmRequest.class);

        force.provision(10, 0, 0, "centos-7", "cpanel", shopperId, 1, UUID.randomUUID());
    }

}
