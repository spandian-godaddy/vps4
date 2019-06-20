package com.godaddy.hfs.dns;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/*
{
  "pagination": {
    "count": 0,
    "last": "string",
    "next": "string",
    "page": 0,
    "previous": "string",
    "total": 0
  },
  "results": [
    {
      "ip_address": "string",
      "name": "string"
    }
  ]
}
 */
public class RdnsRecords {

    public Pagination pagination;
    public Results[] results;

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public Results[] getResults() {
        return results;
    }

    public void setResults(Results[] results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static class Pagination {
        long count;
        long last;
        long next;
        long page;
        long previous;
        long total;

        public Pagination() {
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }

    public static class Results {

        public String ip_address;
        public String name;

        public Results() {
            super();
        }

        public String getIp_address() {
            return ip_address;
        }

        public void setIp_address(String ip_address) {
            this.ip_address = ip_address;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }
}
