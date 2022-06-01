package com.godaddy.vps4.oh;

/*
{
  "status": "ok",
  "response": {
    "count": 1,
    "next": null,
    "data": {
      ...
    }
  }
}
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class OhResponse<T> {
    private T value;

    @JsonSetter("response")
    public void setValue(Response<T> response) {
        this.value = response.value;
    }

    private static class Response<T> {
        @JsonProperty("data")
        public T value;
    }

    public T value() {
        return value;
    }
}
