package com.stoplicht_controller.stoplicht_controller;

import com.stoplicht_controller.stoplicht_controller.Controllers.TrafficlightController;
import com.stoplicht_controller.stoplicht_controller.messaging.TrafficlightStatePublisher;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class StoplichtControllerApplication {

    @Autowired
    private TrafficlightStatePublisher trafficlightStatePublisher;
    @Autowired
    private TrafficlightController trafficlightController;

    public static void main(String[] args) {
        SpringApplication.run(StoplichtControllerApplication.class, args);
    }

    @PostConstruct
    public void init() {
        trafficlightController.start();
    }
}