package com.godaddy.vps4.shopperNotes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachineService;

public class DefaultShopperNotesService implements ShopperNotesService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultShopperNotesService.class);
    private final Config config;
    private final ShopperNotesClientService shopperNotesClientService;
    private final CreditService creditService;
    private final VirtualMachineService virtualMachineService;

    @Inject
    public DefaultShopperNotesService(Config config,
                                      ShopperNotesClientService shopperNotesClientService,
                                      CreditService creditService,
                                      VirtualMachineService virtualMachineService) {
        this.config = config;
        this.shopperNotesClientService = shopperNotesClientService;
        this.creditService = creditService;
        this.virtualMachineService = virtualMachineService;
    }

    @Override
    public UUID processShopperMessage(UUID vmId, String note) {
        // Currently, shopper notes api only works in stage/prod due to different certificate issuers
        String shopperNotesApi = config.get("shopper.notes.api.url", null);
        if(shopperNotesApi == null) {
            return null;
        }
        UUID orionGuid = virtualMachineService.getOrionGuidByVmId(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);

        ShopperNoteRequest request = new ShopperNoteRequest();
        request.plId = credit.getResellerId();
        request.shopperId = credit.getShopperId();
        request.enteredBy = config.get("shopper.notes.enteredBy");
        request.shopperNote = note;
        request.requestingIp = getRequestingIp();
        request.enteredDateTime = getTimeAndDate();

        return shopperNotesClientService.processShopperMessage(request);
    }

    private String getRequestingIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("Unable to determine host address: ", e);
            return "";
        }
    }

    private String getTimeAndDate() {
        String dateTimePattern = config.get("shopper.notes.datetime.pattern");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateTimePattern);
        Instant nowUtc = Instant.now();
        ZoneId azTime = ZoneId.of("-07:00");
        return dtf.format(ZonedDateTime.ofInstant(nowUtc, azTime));
    }
}
