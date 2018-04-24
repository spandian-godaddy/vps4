package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.messaging.DefaultVps4MessagingService.EmailTemplates;
import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;

import java.io.IOException;
import java.time.Instant;

public class ScheduledMaintenanceEmailRequest extends EmailRequest {
    public String accountName;
    public Instant startTime;
    public long durationMinutes;
    public boolean isFullyManaged;

    public ScheduledMaintenanceEmailRequest(EmailTemplates template, String shopperId, String accountName,
                                            boolean isFullyManaged, Instant startTime, long durationMinutes) {
        super(template, shopperId);
        this.accountName = accountName;
        this.isFullyManaged = isFullyManaged;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
    }

    public String sendEmail(Vps4MessagingService messagingService) throws IOException, MissingShopperIdException {
        String messageId = "";

        switch (this.template) {
            case VPS4ScheduledPatchingV2:
                messageId = messagingService.sendScheduledPatchingEmail(this.shopperId, this.accountName,
                        this.startTime, this.durationMinutes, this.isFullyManaged);
                break;
            case VPS4UnexpectedbutScheduledMaintenanceV2:
                messageId = messagingService.sendUnexpectedButScheduledMaintenanceEmail(this.shopperId, this.accountName,
                        this.startTime, this.durationMinutes, this.isFullyManaged);
                break;
            default:
                String message = String.format("Unknown template: %s", this.template);
                throw new UnknownEmailTemplateException(message);
        }

        return messageId;
    }
}
