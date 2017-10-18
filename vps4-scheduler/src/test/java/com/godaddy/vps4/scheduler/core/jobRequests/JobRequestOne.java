package com.godaddy.vps4.scheduler.core.jobRequests;

import com.godaddy.vps4.scheduler.core.JobRequest;
import com.godaddy.vps4.scheduler.core.Optional;
import com.godaddy.vps4.scheduler.core.Required;
import com.godaddy.vps4.scheduler.core.Vps4JobRequestValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class JobRequestOne extends JobRequest {
    private static final Logger logger = LoggerFactory.getLogger(JobRequestOne.class);

    @Required public Integer jobParamOne;
    @Optional public UUID jobParamTwo;
    @Optional public String jobParamThree;

    private void validateJobParamOne() throws Vps4JobRequestValidationException {
        if (jobParamOne == 2) {
            throw new Vps4JobRequestValidationException("INVALID_PARAM_ONE", "Invalid value");
        }
    }

    private void validateJobParamThree() throws Vps4JobRequestValidationException {
        if (jobParamThree.equals("hello")) {
            throw new Vps4JobRequestValidationException("INVALID_PARAM_THREE", "Invalid value");
        }
    }

    @Override
    protected void validate() throws Exception {
        super.validate();
        if(jobParamOne == 3 && jobParamThree.equals("fail")) {
            throw new Vps4JobRequestValidationException(
                    "OBJ_LVL_FAIL",
                    "Object level validation failed");
        }
    }
}
