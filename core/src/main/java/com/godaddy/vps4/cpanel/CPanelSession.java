package com.godaddy.vps4.cpanel;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class CPanelSession {
    public class MetaData {
        public int result;
        public String reason;
        public String command;
        public int version;
    }

    public class Data {
        @JsonAlias("cp_security_token")
        public String cpSecurityToken;
        public String service;
        private Instant expires;
        public String session;
        public String url;

        public Instant getExpires() {
            return expires;
        }

        public void setExpires(int epochSeconds) {
            expires = Instant.ofEpochSecond(epochSeconds);
        }
    }

    public Data data;
    public MetaData metadata;
}
