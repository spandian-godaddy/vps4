package com.godaddy.vps4.orchestration.messaging;

import gdg.hfs.orchestration.CommandContext;

public class SendMessagingEmailBase {
    protected void waitForMessageComplete(CommandContext context, String messageId, String shopperId) {
        if (messageId.isEmpty()) {
            String exceptionMessage = String.format("Error sending email, messageId is empty for shopper %s",
                    shopperId);
            throw new RuntimeException(exceptionMessage);
        }

        // Introduce a wait before getting the message status since the GD messaging api inserts the record into
        // their on box cache but the message is queued up asynchronously to be persisted to their db
        context.sleep(3000);
        context.execute(WaitForMessageComplete.class, messageId);
    }

    public static class Response {
        public String messageId;
    }
}
