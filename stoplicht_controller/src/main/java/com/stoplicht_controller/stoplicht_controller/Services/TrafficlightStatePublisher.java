package com.stoplicht_controller.stoplicht_controller.services;

import com.stoplicht_controller.stoplicht_controller.Entities.TrafficLights;
import com.stoplicht_controller.stoplicht_controller.Enums.TrafficlightState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrafficlightStatePublisher {

    @Autowired
    private ZmqPublisher publisher;
    @Autowired
    private TrafficLights trafficLights;


    public TrafficlightStatePublisher(ZmqPublisher publisher, TrafficLights trafficLights) {
        this.publisher = publisher;
        this.trafficLights = trafficLights;
    }

    public void publish() {
        publisher.sendMessage("stoplichten", trafficLights.toString());
    }

}
