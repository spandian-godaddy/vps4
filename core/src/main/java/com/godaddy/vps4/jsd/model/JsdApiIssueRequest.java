package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JsdApiIssueRequest {
    @JsonProperty("fields") public JsdIssueFields fields;

    public JsdApiIssueRequest(JsdIssueFields fields) {
        this.fields = fields;
    }

    public static class JsdIssueFields {
        @JsonProperty("issuetype") public JsdFieldName issueType;
        @JsonProperty("project") public JsdFieldKey project;
        @JsonProperty("reporter") public JsdFieldId reporter;
        @JsonProperty("summary") public String summary;
        @JsonProperty("customfield_10042")  public String partnerCustomerKey;
        @JsonProperty("customfield_10165") public String plid;
        @JsonProperty("customfield_10197") public JsdFieldValue serviceRequestCategory;
        @JsonProperty("customfield_10207") public String fqdn;
        @JsonProperty("customfield_10223") public JsdFieldValue supportTier;
        @JsonProperty("customfield_10224") public JsdFieldValue severity;
        @JsonProperty("customfield_10229") public String shopperId;
        @JsonProperty("customfield_10230") public String outageId;
        @JsonProperty("customfield_10231") public String outageIdUrl;
        @JsonProperty("customfield_10233") public JsdFieldValue supportProduct;
        @JsonProperty("customfield_10234") public JsdFieldValue customerProduct;
        @JsonProperty("customfield_10247") public String orionGuid;
        @JsonProperty("customfield_10259") public String metricTypes;
        @JsonProperty("customfield_10262") public JsdContentDoc servicesAffected;
        @JsonProperty("customfield_10264") public JsdFieldValue dataCenter;
        @JsonProperty("description") public JsdContentDoc description;

        public JsdIssueFields() {
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
