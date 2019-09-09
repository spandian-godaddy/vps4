package com.godaddy.vps4.panopta;

import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "customer_list": [
    {
      "customer_key": "2hum-wpmt-vswt-2g3b",
      "email_address": "abhoite@godaddy.com",
      "name": "Godaddy VPS4 POC",
      "package": "godaddy.fully_managed",
      "partner_customer_key": "godaddy_177999314",
      "status": "active",
      "url": "https://api2.panopta.com/v2/customer/2hum-wpmt-vswt-2g3b"
    }
  ],
  "meta": {
    "limit": 50,
    "next": null,
    "offset": 0,
    "previous": null,
    "total_count": 1
  }
}
*/
public class PanoptaApiCustomerList {

    @JsonProperty("customer_list")
    private List<Customer> customerList;

    public Meta meta;

    public List<Customer> getCustomerList() {
        return customerList;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public static class Meta {
        public int limit;
        public int offset;
        public String next;
        public String previous;
        @JsonProperty("total_count")
        public int totalCount;

        public Meta() {
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class Customer {

        @JsonProperty("customer_key")
        public String customerKey;
        @JsonProperty("email_address")
        public String emailAddress;
        public String name;
        @JsonProperty("package")
        public String panoptaPackage;
        @JsonProperty("partner_customer_key")
        public String partnerCustomerKey;
        public String status;
        public String url;

        public Customer() { super(); }

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
