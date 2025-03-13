package com.stoplicht_controller.stoplicht_controller;

import com.stoplicht_controller.stoplicht_controller.Services.TrafficlightStatePublisher;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StoplichtControllerApplication {

    @Autowired
    private TrafficlightStatePublisher trafficlightStatePublisher;


    public static void main(String[] args) {
        SpringApplication.run(StoplichtControllerApplication.class, args);
    }

    @PostConstruct
    public void init() {
        publishTrafficLightState();
    }
    // You can create a method to publish the traffic light state
    public void publishTrafficLightState() {
        while(true) {
            trafficlightStatePublisher.publish();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
