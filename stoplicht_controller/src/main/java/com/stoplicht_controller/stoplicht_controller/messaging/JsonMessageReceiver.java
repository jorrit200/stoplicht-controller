package com.stoplicht_controller.stoplicht_controller.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonMessageReceiver {
    @Autowired
    private final ZmqSubscriber subscriber;
    private final ObjectMapper mapper;

    @Autowired
    public JsonMessageReceiver(ZmqSubscriber subscriber, ObjectMapper mapper) {
        this.subscriber = subscriber;
        this.mapper = mapper;
    }

    public <T> T receiveMessage(String topic, Class<T> clazz) {
        try {
            String json = subscriber.receiveMessage(topic);
            System.out.println(json);
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            System.err.println("Error receiving or parsing message: " + e.getMessage());
            e.printStackTrace();
            return receiveMessage(topic, clazz);
        }
    }
}
