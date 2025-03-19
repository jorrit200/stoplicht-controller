package com.stoplicht_controller.stoplicht_controller.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonMessageReceiver {
    private final ZmqSubscriber subscriber;
    private final ObjectMapper mapper;

    @Autowired
    public JsonMessageReceiver(ZmqSubscriber subscriber, ObjectMapper mapper) {
        this.subscriber = subscriber;
        this.mapper = mapper;
    }

    public <T> T receiveMessage(String topic, Class<T> clazz) throws Exception {
        String json = subscriber.receiveMessage(topic);
        return mapper.readValue(json, clazz);
    }
}
