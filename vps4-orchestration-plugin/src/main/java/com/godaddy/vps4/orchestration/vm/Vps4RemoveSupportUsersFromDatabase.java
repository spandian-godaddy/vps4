package com.godaddy.vps4.orchestration.vm;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(name = "Vps4RemoveSupportUsersFromDatabase", requestType = UUID.class, retryStrategy = CommandRetryStrategy.NEVER)
public class Vps4RemoveSupportUsersFromDatabase implements Command<UUID, Void> {

    private final VmUserService vmUserService;

    private UUID vmId;

    @Inject
    public Vps4RemoveSupportUsersFromDatabase(VmUserService vmUserService) {
        this.vmUserService = vmUserService;
    }


    @Override
    public Void execute(CommandContext context, UUID vmId) {
        this.vmId = vmId;
        deleteAllSupportUsers();
        return null;
    }

    private void deleteAllSupportUsers() {
        List<VmUser> users = vmUserService.listUsers(vmId, VmUserType.SUPPORT);

        for(VmUser user : users) {
            vmUserService.deleteUser(user.username, vmId);
        }
    }
}
