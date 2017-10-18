package com.godaddy.vps4.scheduler.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract public class JobRequestBase {

    private static final Logger logger = LoggerFactory.getLogger(JobRequestBase.class);

    @JsonIgnore private final List<Vps4JobRequestValidationException> exceptions = new ArrayList<>();
    @JsonIgnore public final List<Vps4JobRequestValidationException> getExceptions() {
        return exceptions;
    }

    @JsonIgnore
    private Method findMethodByName(String methodName) {
        // walk the inheritance hierarchy up to the JobRequest class looking for the method.
        // A private method defined in a super class is not visible via the getDeclaredMethod call
        // Hence, this!!
        for(Class<?> current = this.getClass();
            current!=null && JobRequestBase.class.isAssignableFrom(current);
            current=current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(methodName);
            }
            catch (NoSuchMethodException e) {
                // no-op. this is ok as this is a valid possibility. we are looking for a matching anywhere in the
                // hierarchy.
            }
        }

        return null;
    }

    protected void validate() throws Exception {
        // no-op
    }

    private void runRequiredFieldsCheck() throws Exception {
        Class<? extends JobRequestBase> jobRequestClass = this.getClass();
        Arrays.stream(jobRequestClass.getFields())
            .filter(field -> field.isAnnotationPresent(Required.class))
            .forEach(field -> {
                try {
                    // Check if this field is set on the object
                    if (field.get(this) == null) {
                        exceptions.add(
                            new Vps4JobRequestValidationException(
                                "REQD_FIELD_MISSING",
                                String.format("Field '%s' missing", field.getName())));
                    }
                }
                catch (IllegalAccessException e) {
                    // don't count this exception as a validation specific exception?
                    logger.error("IllegalAccessException {}", e);
                }
            });

        if (exceptions.size() > 0) {
            throw new Exception("Required field checks failed");
        }
    }

    private void runFieldLevelValidations() throws Exception {
        Class<? extends JobRequestBase> jobRequestClass = this.getClass();
        Arrays.stream(jobRequestClass.getFields())
            .filter(field -> {
                try {
                    return field.get(this) != null;
                }
                catch (IllegalAccessException e) {
                    logger.error("exception {}", e);
                    return false;
                }
            })
            .map(field -> {
                String fieldName = StringUtils.capitalize(field.getName());
                String validateMethodName = String.format("validate%s", fieldName);
                return findMethodByName(validateMethodName);
            })
            .filter(method -> method != null)
            .forEach(method -> {
                try {
                    method.setAccessible(true);
                    method.invoke(this);
                }
                catch (InvocationTargetException e) {
                    exceptions.add((Vps4JobRequestValidationException) e.getTargetException());
                }
                catch (IllegalAccessException e) {
                    // don't count this exception as a validation specific exception
                    logger.error("IllegalAccessException {}", e);
                }
            });

        if (exceptions.size() > 0) {
            throw new Exception("Field level validation checks failed");
        }
    }

    private void runObjectLevelValidations() throws Exception {
        Method method = findMethodByName("validate");
        try {
            if (method != null) {
                method.setAccessible(true);
                method.invoke(this);
            }
        }
        catch (InvocationTargetException e) {
            exceptions.add((Vps4JobRequestValidationException) e.getTargetException());
        }
        catch (IllegalAccessException e) {
            // don't count this exception as a validation specific exception
            logger.error("IllegalAccessException {}", e);
        }

        if (exceptions.size() > 0) {
            throw new Exception("Object level validation checks failed");
        }
    }

    @JsonIgnore
    public final boolean isValid() {
        try {
            runRequiredFieldsCheck();
            runFieldLevelValidations(); // run field level validations
            runObjectLevelValidations(); // run object level validations
        }
        catch (Exception e) {
            // no-op
        }

        return exceptions.size() == 0;
    }
}
