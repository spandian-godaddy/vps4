package com.godaddy.vps4.orchestration.network;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.ipblacklist.IpBlacklistService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "RemoveIpFromBlacklist",
        requestType = String.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class RemoveIpFromBlacklist implements Command<String, Void> {
    private final IpBlacklistService ipBlacklistService;

    @Inject RemoveIpFromBlacklist(IpBlacklistService ipBlacklistService) {this.ipBlacklistService = ipBlacklistService;}

    @Override
    public Void execute (CommandContext context, String ipAddress) {
        ipBlacklistService.removeIpFromBlacklist(ipAddress);
        return null;
    }
}
