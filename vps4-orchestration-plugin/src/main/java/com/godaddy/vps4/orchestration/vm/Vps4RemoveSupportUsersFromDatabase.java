package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.google.inject.Inject;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@CommandMetadata(
        name="Vps4RemoveSupportUsersFromDatabase",
        requestType=UUID.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RemoveSupportUsersFromDatabase implements Command<UUID, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RemoveSupportUsersFromDatabase.class);

    private final VmUserService vmUserService;

    private CommandContext context;
    private UUID vmId;

    @Inject
    public Vps4RemoveSupportUsersFromDatabase(VmUserService vmUserService) {
        this.vmUserService = vmUserService;
    }


    @Override
    public Void execute(CommandContext context, UUID vmId) {
        this.context = context;
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
