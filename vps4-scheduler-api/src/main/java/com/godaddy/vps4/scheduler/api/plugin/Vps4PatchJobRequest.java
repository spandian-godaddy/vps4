package com.godaddy.vps4.scheduler.api.plugin;

import com.godaddy.vps4.scheduler.api.core.JobGroup;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.Product;
import com.godaddy.vps4.scheduler.api.core.Required;

import java.util.UUID;

@Product("vps4")
@JobGroup("patch")
public class Vps4PatchJobRequest extends JobRequest {
    @Required public UUID vmId;
}
