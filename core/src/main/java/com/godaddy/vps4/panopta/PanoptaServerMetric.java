package com.godaddy.vps4.panopta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
{"data": {"Memory (kB)": [{"data": [503869, ...,
 505361.6, 504952.0, 504952.0], "name": "RAM used (kB) (kB)", "unit": "kB"}]},
 "end_time": "2019-07-23 16:04:24",
 "labels": [1563811464.0,...1563896664.0, 1563897264.0, 1563897864.0],
  "start_time": "2019-07-22 16:04:24",
  "timescale": "day",
   "url": "https://api2.panopta.com/v2/server/1105540/agent_resource/11483466"}
*/

public class PanoptaServerMetric {

    public PanoptaServerMetricData data;
    @JsonProperty("end_time")
    public String endTime;
    public List<String> labels;
    @JsonProperty("start_time")
    public String startTime;
    public String timescale;
    public String url;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}

class PanoptaServerMetricData{
    public Map<String, PanoptaData[]> metrics = new HashMap<String, PanoptaData[]>();

    @JsonAnySetter
    public void setDetail(String key, PanoptaData[] value) {
        metrics.put(key, value);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}


class PanoptaData{
    public List<Float> data;
    public String name;
    public String unit;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
