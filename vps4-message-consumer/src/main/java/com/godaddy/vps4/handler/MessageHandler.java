package com.godaddy.vps4.handler;

@FunctionalInterface
public interface MessageHandler {
    
    void handleMessage(String message) throws MessageHandlerException;
}

