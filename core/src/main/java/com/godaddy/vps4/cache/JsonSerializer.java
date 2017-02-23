package com.godaddy.vps4.cache;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.nio.serialization.ByteArraySerializer;

public class JsonSerializer implements ByteArraySerializer<Object> {

    final ObjectMapper mapper;

    public JsonSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public byte[] write(Object o) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(o);

        //System.out.println("bytes: " + new String(bytes, "UTF-8"));

        return bytes;
    }

    @Override
    public Object read(byte[] buffer) throws IOException {

        return mapper.readValue(buffer, Object.class);
    }

    @Override
    public int getTypeId() {
        return 1000;
    }

    @Override
    public void destroy() {

    }

}
