package com.godaddy.vps4.panopta;

import static com.godaddy.vps4.panopta.PanoptaGraph.Type.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "agent_resource_list": [
    {
      "agent_resource_type": "https://api2.panopta.com/v2/agent_resource_type/675",
      "formatted_name": "Memory: RAM percent usage",
      "frequency": 60,
      "name": "Memory: RAM percent usage",
      "name_override": "",
      "plugin_textkey": "memory",
      "resource_option": {},
      "resource_textkey": "ram.percent",
      "status": "active",
      "tags": [],
      "template": "https://api2.panopta.com/v2/server_template/1146058",
      "template_agent_resource": "https://api2.panopta.com/v2/server/1146058/agent_resource/12994971",
      "thresholds": [],
      "url": "https://api2.panopta.com/v2/server/1151747/agent_resource/13321688"
    },[...]
  ]
}
*/

public class PanoptaUsageIdList {
    @JsonProperty("agent_resource_list")
    private List<UsageId> usageIdList = new ArrayList<>();

    public List<PanoptaGraphId> getList() {
        usageIdList.removeIf(e -> e.type == UNKNOWN);
        return new ArrayList<>(usageIdList);
    }

    public void setList(List<PanoptaGraphId> list) {
        this.usageIdList = new ArrayList<>();
        for (PanoptaGraphId id : list) {
            UsageId usageId = new UsageId();
            usageId.id = id.id;
            usageId.type = id.type;
            this.usageIdList.add(usageId);
        }
    }

    public static class UsageId extends PanoptaGraphId {
        @JsonProperty("resource_textkey")
        public void mapResourceKey(String key) {
            switch (key) {
                case "ram.percent":
                    this.type = RAM;
                    break;
                case "usage_percentage":
                    this.type = CPU;
                    break;
                case "usage.percent_used":
                    this.type = DISK;
                    break;
                default:
                    this.type = UNKNOWN;
                    break;
            }
        }

        @JsonProperty("url")
        public void mapUrl(String url) {
            this.id = Integer.parseInt(url.split("/")[7]);
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