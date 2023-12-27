package com.godaddy.vps4.cdn.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CdnClientUpdateRequest {
    public String cacheLevel;
    public String bypassWAF;
}
