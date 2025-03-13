package com.stoplicht_controller.stoplicht_controller;

import com.stoplicht_controller.stoplicht_controller.Entities.TrafficLights;
import com.stoplicht_controller.stoplicht_controller.Services.TrafficlightStatePublisher;
import com.stoplicht_controller.stoplicht_controller.Services.ZmqPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StoplichtControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoplichtControllerApplication.class, args);

        TrafficlightStatePublisher trafficlightStatePublisher;

        trafficlightStatePublisher.publish();
    }

}
