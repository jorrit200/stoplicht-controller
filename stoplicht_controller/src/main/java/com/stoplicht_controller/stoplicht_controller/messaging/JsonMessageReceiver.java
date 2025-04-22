package com.stoplicht_controller.stoplicht_controller.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqSubscriber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    @PostConstruct
    public void init() {
        subscriber.subscribeTopics(List.of(
                "sensoren_rijbaan",
                "tijd",
                "voorrangsvoertuig",
                "sensoren_speciaal"
        ));
    }
    public <T> T receiveMessage(Class<T> clazz) {
        try {
            String[] received = subscriber.receiveMessage();
            String json = received[1];

            System.out.println("Ontvangen Topic: " + received[0]);
            System.out.println("Ontvangen JSON: " + json);

            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
