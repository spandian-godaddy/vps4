package com.godaddy.vps4.web.vm;

import java.util.Random;
import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineSpec;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.security.AutoCreateVps4UserModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class CreateTestVmRequest {

    final VirtualMachineService virtualMachineService;

    final ProjectService projectService;

    final ImageService imageService;

    final Vps4UserService userService;

    @Inject
    public CreateTestVmRequest(
            VirtualMachineService virtualMachineService,
            ProjectService projectService,
            ImageService imageService,
            Vps4UserService userService) {
        this.virtualMachineService = virtualMachineService;
        this.projectService = projectService;
        this.imageService = imageService;
        this.userService = userService;
    }

    protected void provision(int tier, int managedLevel, String operatingSystem, String controlPanel, String shopperId) {

        VirtualMachineSpec spec = virtualMachineService.getSpec(tier);

        UUID orionGuid = UUID.randomUUID();
        virtualMachineService.createVirtualMachineRequest(orionGuid, operatingSystem, controlPanel, tier, managedLevel, shopperId);

        // normally we would get this from HFS
        long vmId = new Random().nextInt(1000000);

        Vps4User user = userService.getOrCreateUserForShopper(shopperId);

        long projectId = projectService.createProject("My Cool Project", user.getId(), 1).getProjectId();

        int imageId = imageService.getImageId(operatingSystem);

        virtualMachineService.provisionVirtualMachine(vmId, orionGuid, "SomeNewVm", projectId, spec.specId, managedLevel, imageId);
    }

    public static void main(String[] args) {

        final String shopperId = "5y5";

        Injector injector = Guice.createInjector(
                new ConfigModule(), new DatabaseModule(),
                new VmModule(), new SecurityModule(), new AutoCreateVps4UserModule(shopperId));

        CreateTestVmRequest force = injector.getInstance(CreateTestVmRequest.class);

        force.provision(10, 0, "centos-7", "cpanel", shopperId);
    }

}
