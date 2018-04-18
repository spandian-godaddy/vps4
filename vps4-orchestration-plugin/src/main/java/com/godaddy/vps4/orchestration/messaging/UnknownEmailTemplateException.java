package com.godaddy.vps4.orchestration.messaging;

public class UnknownEmailTemplateException extends RuntimeException {
    private static final long serialVersionUID = 20L;

    public UnknownEmailTemplateException(String message, Throwable e) {
        super(message, e);
    }

    public UnknownEmailTemplateException(String message) { super(message); }
}
