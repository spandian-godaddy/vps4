package com.godaddy.vps4.web;

public abstract class Action {

    public enum ActionStatus {
        IN_PROGRESS, COMPLETE, ERROR
    }

    public long actionId;
    public volatile ActionStatus status;
    public volatile String message;
}
