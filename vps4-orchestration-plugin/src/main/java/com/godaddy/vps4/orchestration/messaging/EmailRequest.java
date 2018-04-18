package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.messaging.DefaultVps4MessagingService.EmailTemplates;
import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;

import java.io.IOException;

public abstract class EmailRequest implements IMessagingEmail {
    public EmailTemplates template;
    public String shopperId;

    public EmailRequest(EmailTemplates template, String shopperId) {
        this.template = template;
        this.shopperId = shopperId;
    }

    public abstract String sendEmail(Vps4MessagingService messagingService) throws IOException, MissingShopperIdException;
}
