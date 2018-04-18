package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.messaging.DefaultVps4MessagingService.EmailTemplates;
import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;

import java.io.IOException;

public class FailOverEmailRequest extends EmailRequest {
    public String accountName;
    public boolean isFullyManaged;

    public FailOverEmailRequest(EmailTemplates template, String shopperId, String accountName, boolean isFullyManaged) {
        super(template, shopperId);
        this.accountName = accountName;
        this.isFullyManaged = isFullyManaged;
    }

    public String sendEmail(Vps4MessagingService messagingService) throws IOException, MissingShopperIdException {
        String messageId = "";

        switch (this.template) {
            case VPS4SystemDownFailover:
                messageId = messagingService.sendSystemDownFailoverEmail(this.shopperId, this.accountName,
                        this.isFullyManaged);
                break;
            case VPS4UnexpectedschedmaintFailoveriscompleted:
                messageId = messagingService.sendFailoverCompletedEmail(this.shopperId, this.accountName,
                        this.isFullyManaged);
                break;
            default:
                String message = String.format("Unknown template: %s", this.template);
                throw new UnknownEmailTemplateException(message);
        }

        return messageId;
    }
}
