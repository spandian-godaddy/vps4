package com.godaddy.vps4.handler;

public enum MessageNotificationType {
    SUSPENDED,
    ABUSE_SUSPENDED,
    REMOVED,
    REINSTATED,
    ADDED,
    UPDATED,
    RENEWED,
    OTHER;

    public static MessageNotificationType getEnum(String messageType){
        for(MessageNotificationType mnType : MessageNotificationType.values()){
            if(mnType.toString().equalsIgnoreCase(messageType)){
                return mnType;
            }
        }
        return MessageNotificationType.OTHER;
    }

}
