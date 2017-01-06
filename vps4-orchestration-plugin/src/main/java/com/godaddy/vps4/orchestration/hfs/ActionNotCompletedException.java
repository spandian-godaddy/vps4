package com.godaddy.vps4.orchestration.hfs;

public class ActionNotCompletedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    final Object action;

    public ActionNotCompletedException(Object action) {
        this.action = action;
    }

    public Object getAction() {
        return action;
    }
}
