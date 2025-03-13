package com.stoplicht_controller.stoplicht_controller.Services;

import com.stoplicht_controller.stoplicht_controller.Entities.TrafficLights;
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
