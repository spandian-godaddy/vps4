package com.godaddy.vps4.scheduler.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SchedulerClientService {

    SchedulerResponse submitJobToGroup(String product, String jobGroup, RequestBody requestJson);

    List<SchedulerResponse> getGroupJobs(String product, String jobGroup);

    SchedulerResponse getJob(String product, String jobGroup, UUID jobId);

    SchedulerResponse rescheduleJob(String product, String jobGroup, UUID jobId, RequestBody requestJson);

    void deleteJob(String product, String jobGroup, UUID jobId);

    static class RequestBody {
        public UUID getVmId() {
            return vmId;
        }

        public void setVmId(UUID vmId) {
            this.vmId = vmId;
        }

        public Instant getWhen() {
            return when;
        }

        public void setWhen(Instant when) {
            this.when = when;
        }

        public UUID vmId;
        public Instant when;

    }

}

