package com.godaddy.vps4.handler;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JsonMessage {

    public String key;
    public JSONObject value;

    private JSONParser parser = new JSONParser();

    public JsonMessage(ConsumerRecord<String, String> message) throws MessageHandlerException {
        this.key = message.key();
        this.value = parseMessage(message.value());
    }

    private JSONObject parseMessage(String value) throws MessageHandlerException {
        try {
            return (JSONObject) parser.parse(value);
        } catch (ParseException ex) {
            throw new MessageHandlerException("Could not parse kafka message value, expected JSON value", ex);
        }
    }

}
