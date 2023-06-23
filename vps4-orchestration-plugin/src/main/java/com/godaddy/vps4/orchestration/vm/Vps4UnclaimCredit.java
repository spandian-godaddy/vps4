package com.godaddy.vps4.orchestration.vm;

import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuotaAndCount;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class Vps4UnclaimCredit implements Command<VirtualMachine, Void> {
    private static final Logger logger = LoggerFactory.getLogger(SetMailRelayQuotaAndCount.class);

    private final CreditService creditService;
    private final MailRelayService mailRelayService;

    @Inject
    public Vps4UnclaimCredit(CreditService creditService, MailRelayService mailRelayService) {
        this.creditService = creditService;
        this.mailRelayService = mailRelayService;
    }

    @Override
    public Void execute(CommandContext commandContext, VirtualMachine vm) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        if (credit.getProductId() != null && credit.getProductId().equals(vm.vmId)) {
            int mailRelays = getMailRelays(vm);
            creditService.unclaimVirtualMachineCredit(vm.orionGuid, vm.vmId, mailRelays);
        }
        return null;
    }

    private int getMailRelays(VirtualMachine vm) {
        if (vm.primaryIpAddress == null) {
            return 0;
        }

        int mailRelays = 0;
        if (vm.spec.serverType.serverType == ServerType.Type.VIRTUAL) {
            try {
                MailRelay relay = mailRelayService.getMailRelay(vm.primaryIpAddress.ipAddress);
                mailRelays = relay.relays;
            } catch (NotFoundException e) {
                logger.debug("Mail relay record not found for ip {}", vm.primaryIpAddress.ipAddress);
            } catch (Exception e) {
                logger.debug("Failed to find mail relays for vm {}, setting mail relay count to 0", vm.vmId);
            }
        }
        return mailRelays;
    }
}
