package com.godaddy.vps4.messaging;


import com.godaddy.vps4.messaging.models.Message;

import java.io.IOException;

public interface Vps4MessagingService {

    Message getMessageById(String messageId)
            throws IOException;

    String sendSetupEmail(String shopperId, String accountName, String ipAddress, String diskSpace)
            throws IOException;
}
