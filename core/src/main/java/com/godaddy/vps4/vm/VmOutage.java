package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class VmOutage {
    public UUID vmId;
    public Set<VmMetric> metrics;
    public Instant started;
    public Instant ended;
    public String reason;
    public String severity;
    public long panoptaOutageId;
    public List<DomainMonitoringMetadata> domainMonitoringMetadata;

    public static class DomainMonitoringMetadata {
        public String additionalFqdn;
        public List<String> metadata;
        public VmMetric metric;

        // needed for deserializer
        public DomainMonitoringMetadata() {
        }

        public DomainMonitoringMetadata(String additionalFqdn, List<String> metadata, VmMetric metric) {
            this.additionalFqdn = additionalFqdn;
            this.metadata = metadata;
            this.metric = metric;
        }

    }

    public String metricTypeMapper() {
        String metricString = metrics.stream().filter(m -> m != VmMetric.HTTP && m != VmMetric.HTTPS ).map(VmMetric::name)
                .collect(Collectors.joining(", "));

        metricString += domainMonitoringMetadata.size() >= 1 ? ", " + domainMonitoringMetadata.stream().map(m -> m.metric.toString() + " (" + m.additionalFqdn +")")
                .collect(Collectors.joining(", ")) : "";

        return metricString;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
