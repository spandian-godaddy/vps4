package com.godaddy.vps4.handler;

@FunctionalInterface
public interface Vps4MessageHandler {
    
    void handleMessage(String message) throws Vps4MessageHandlerException;
}

