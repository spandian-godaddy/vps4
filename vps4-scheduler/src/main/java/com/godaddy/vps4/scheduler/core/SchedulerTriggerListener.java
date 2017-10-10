package com.godaddy.vps4.scheduler.core;

import org.quartz.listeners.TriggerListenerSupport;

public abstract class SchedulerTriggerListener extends TriggerListenerSupport {
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
