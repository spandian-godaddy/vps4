package com.godaddy.vps4.scheduler.api.core.jobRequests;

import java.util.UUID;

import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.Optional;
import com.godaddy.vps4.scheduler.api.core.Required;
import com.godaddy.vps4.scheduler.api.core.Vps4JobRequestValidationException;

public class JobRequestOne extends JobRequest {

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
