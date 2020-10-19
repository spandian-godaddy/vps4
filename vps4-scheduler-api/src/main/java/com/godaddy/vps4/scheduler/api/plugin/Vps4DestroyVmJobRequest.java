package com.godaddy.vps4.scheduler.api.plugin;

import java.util.UUID;

import com.godaddy.vps4.scheduler.api.core.JobGroup;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.Product;
import com.godaddy.vps4.scheduler.api.core.Required;

@Product("vps4")
@JobGroup("destroyVm")
public class Vps4DestroyVmJobRequest extends JobRequest {
    @Required public UUID vmId;
}