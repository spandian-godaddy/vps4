package com.godaddy.vps4.scheduler.api.core;

import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class Vps4JobRequestValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String id;

    public Vps4JobRequestValidationException(String id, String message) {
        super(message);
        this.id = id;
    }

    public Vps4JobRequestValidationException(String id, String message, Throwable cause) {
        super(message, cause);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, MultilineRecursiveToStringStyle.SHORT_PREFIX_STYLE);
    }

}
