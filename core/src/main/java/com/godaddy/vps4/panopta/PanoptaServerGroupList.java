package com.godaddy.vps4.panopta;

import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "meta": {
    "limit": 50,
    "next": null,
    "offset": 0,
    "previous": null,
    "total_count": 1
  },
  "server_group_list": [
    {
      "name": "Default Server Group",
      "notification_schedule": "https://api2.panopta.com/v2/notification_schedule/224324",
      "server_group": null,
      "tags": [],
      "url": "https://api2.panopta.com/v2/server_group/428250"
    }
  ]
}
*/

public class PanoptaServerGroupList {
    @JsonProperty("server_group_list")
    public List<PanoptaServerGroup> groups;

    public static class PanoptaServerGroup {
        public String name;
        public String url;

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
