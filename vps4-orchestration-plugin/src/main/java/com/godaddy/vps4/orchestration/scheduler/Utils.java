package com.godaddy.vps4.orchestration.scheduler;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServerErrorException;

import org.slf4j.Logger;

import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.api.plugin.Vps4CancelAccountJobRequest;
import com.godaddy.vps4.scheduler.api.plugin.Vps4DestroyVmJobRequest;
import com.godaddy.vps4.scheduler.api.plugin.Vps4RemoveSupportUserJobRequest;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;

import gdg.hfs.orchestration.CommandContext;

public abstract class Utils {
    private static final Map<ScheduledJob.ScheduledJobType, Class<? extends JobRequest>> typeClassMap = new HashMap<>();
    static {
        typeClassMap.put(ScheduledJob.ScheduledJobType.BACKUPS_RETRY, Vps4BackupJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.ZOMBIE, Vps4ZombieCleanupJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.REMOVE_SUPPORT_USER, Vps4RemoveSupportUserJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.BACKUPS_MANUAL, Vps4BackupJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC, Vps4BackupJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.DESTROY_VM, Vps4DestroyVmJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.CANCEL_ACCOUNT, Vps4CancelAccountJobRequest.class);
    }

    public static Class<? extends JobRequest> getJobRequestClassForType(ScheduledJob.ScheduledJobType scheduledJobType) {
        return typeClassMap.get(scheduledJobType);
    }

    public interface RetryActionHandler <T> {
        T handle();
    }

    public static <T> T runWithRetriesForServerErrorException(CommandContext context, Logger logger, RetryActionHandler<T> handler) {
        int serverErrorRetries = 0;
        int maxRetries = 5;
        while (serverErrorRetries < maxRetries) {
            context.sleep(2000);
            try {
                return handler.handle();
            }
            catch (ServerErrorException e) {
                logger.info("Caught Server Error. Attempting retry number: #{}", serverErrorRetries);
                if (++serverErrorRetries >= maxRetries)
                    throw e;
            }
        }
        return null;
    }

    public static <T> T runWithRetriesForServerAndProcessingErrorException(CommandContext context, Logger logger, RetryActionHandler<T> handler, int sleepDuration) {
        int serverErrorRetries = 0;
        int maxRetries = 5;
        while (serverErrorRetries < maxRetries) {
            context.sleep(sleepDuration);
            try {
                return handler.handle();
            }
            catch (ServerErrorException e) {
                logger.info("Caught Server Error. Attempting retry number: #{}", serverErrorRetries);
                if (++serverErrorRetries >= maxRetries)
                    throw e;
            } catch (ProcessingException e) {
                logger.info("Caught Processing Error. Attempting retry number: #{}", serverErrorRetries);
                if (++serverErrorRetries >= maxRetries)
                    throw e;
            }
        }
        return null;
    }

    public static <T> T runWithRetriesForServerAndProcessingErrorException(CommandContext context, Logger logger, RetryActionHandler<T> handler) {
        return runWithRetriesForServerAndProcessingErrorException(context, logger, handler, 2000);
    }
}
