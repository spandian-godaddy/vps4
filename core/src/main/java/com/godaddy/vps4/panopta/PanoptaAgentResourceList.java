package com.godaddy.vps4.panopta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

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

public class PanoptaAgentResourceList {
    public List<PanoptaAgentResource> agent_resource_list;
    public Meta meta;

    public Map<String,Integer> returnAgentResourceIdList() {
        Map<String,Integer> agentResourceIdList = new LinkedHashMap<String,Integer>();
        for(int i = 0;i<agent_resource_list.size();i++){
            PanoptaAgentResource agentResource = agent_resource_list.get(i);
            agentResourceIdList.put(agentResource.resourceTextkey,Integer.parseInt(agentResource.url.split("/")[7]));
        }
        return agentResourceIdList;
    }
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}

class Meta {
    public String limit;
    public String next;
    public String offset;
    public String previous;
    @JsonProperty("total_count")
    public String totalCount;

    public Meta() {
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
class PanoptaAgentResource{
    @JsonProperty("agent_resource_type")
    public String agentResourceType;
    @JsonProperty("formatted_name")
    public String formattedName;
    public int frequency;
    public String name;
    @JsonProperty("name_override")
    public String nameOverride;
    @JsonProperty("plugin_textkey")
    public String pluginTextkey;
    @JsonProperty("resource_option")
    JsonNode resourceOption;
    @JsonProperty("resource_textkey")
    public String resourceTextkey;
    public String status;
    public List<String> tags;
    public String template;
    @JsonProperty("template_agent_resource")
    public String templateAgentResource;
    public List<Thresholds> thresholds;
    public String url;

    public int returnAgentResourceId() {
        return Integer.parseInt(url.split("/")[7]);
    }
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
class Thresholds {
    int duration;
    @JsonProperty("exclude_from_availability")
    boolean excludeFromAvailability;
    @JsonProperty("notification_schedule")
    String notificationSchedule;
    String severity;
    String type;
    int value;
}