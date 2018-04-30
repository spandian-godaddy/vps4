package com.godaddy.vps4.orchestration.messaging;

import gdg.hfs.orchestration.CommandContext;

public class SendMessagingEmailBase {
    protected void waitForMessageComplete(CommandContext context, String messageId, String shopperId) {
        if (messageId.isEmpty()) {
            String exceptionMessage = String.format("Error sending email, messageId is empty for shopper %s",
                    shopperId);
            throw new RuntimeException(exceptionMessage);
        }

        context.execute(WaitForMessageComplete.class, messageId);
    }
}
