package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Arrays;
import java.util.List;

public class JsdApiIssueRequest {
    private static final String CONTENT_MARKS = "strong";
    private static final String CONTENT_TYPE_TEXT = "text";
    private static final String CONTENT_TYPE_PARAGRAPH = "paragraph";
    private static final String CONTENT_TYPE_DOC = "doc";
    private static final int CONTENT_VERSION = 1;
    @JsonProperty("fields") public JSDIssueFields fields;

    public JsdApiIssueRequest(JSDIssueFields fields) {
        this.fields = fields;
    }

    public static class JSDIssueFields {
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
        @JsonProperty("customfield_10262") public ContentDoc servicesAffected;
        @JsonProperty("customfield_10264") public JsdFieldValue dataCenter;
        @JsonProperty("description") public ContentDoc description;

        public JSDIssueFields() {
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class ContentDoc extends ContentParagraph{
        @JsonProperty("version") public Integer version;

        public ContentDoc(List<Object> contentList) {
            super(contentList);
            this.type = CONTENT_TYPE_DOC;
            this.version = CONTENT_VERSION;
        }
        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class ContentParagraph {
        @JsonProperty("content") public List<Object> contentList;
        @JsonProperty("type") public String type;

        public ContentParagraph(List<Object> contentList) {
            this.type = CONTENT_TYPE_PARAGRAPH;
            this.contentList = contentList;
        }
        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class ContentNodeMarks {
        @JsonProperty("type") public String type;

        public ContentNodeMarks(String type){
            this.type = type;
        }
        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class ContentNodeLabel extends ContentNodeValue {
        @JsonProperty("marks") public List<Object> marks;
        public ContentNodeLabel(String text){
            super(text);
            this.marks = Arrays.asList(new ContentNodeMarks(CONTENT_MARKS));
            this.type = CONTENT_TYPE_TEXT;
        }
        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class ContentNodeValue extends ContentNodeMarks{
        @JsonProperty("text") public String text;

        public ContentNodeValue(String text){
            super(CONTENT_TYPE_TEXT);
            this.text = text;
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
