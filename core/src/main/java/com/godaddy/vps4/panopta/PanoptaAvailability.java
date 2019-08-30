package com.godaddy.vps4.panopta;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
{
  "availability": 1,
  "end_time": "2019-08-19 06:00:00",
  "outage_count": 0,
  "start_time": "2019-08-17 12:00:00",
  "total_downtime": 0,
  "with_excluded_outages": null
}
*/

public class PanoptaAvailability {
    public double availability;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}