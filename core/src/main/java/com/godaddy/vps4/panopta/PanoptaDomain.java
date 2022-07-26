package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PanoptaDomain {
    public long id;
    public long typeId;
    public String serverInterface;
    public String status;
    public Instant validOn;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Map<String, String> metadata = new HashMap<>();

    PanoptaDomain(PanoptaMetricId panoptaMetricId, Instant validOn) {
        this.id = panoptaMetricId.id;
        this.typeId = panoptaMetricId.typeId;
        this.serverInterface = panoptaMetricId.serverInterface;
        this.status = panoptaMetricId.status;
        this.validOn = validOn;
    }
}