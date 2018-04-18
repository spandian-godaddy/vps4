package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;

import java.io.IOException;

public interface IMessagingEmail {
    String sendEmail(Vps4MessagingService messagingService) throws IOException, MissingShopperIdException;
}
