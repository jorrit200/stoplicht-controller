package com.stoplicht_controller.stoplicht_controller;

import com.stoplicht_controller.stoplicht_controller.Configurations.TestPublisher;
import com.stoplicht_controller.stoplicht_controller.Controllers.TrafficlightController;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StoplichtControllerApplication {

    @Autowired
    private TrafficlightController trafficlightController;

    @Autowired
    private TestPublisher testPublisher;

    public static void main(String[] args) {
        SpringApplication.run(StoplichtControllerApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Start the TrafficlightController
        new Thread(trafficlightController::start).start();

//        // Start the TestPublisher in a separate thread
//        new Thread(() -> {
//            try {
//                while (true) {
//                    testPublisher.startLoop();
//                    Thread.sleep(1000); // Adjust the sleep time as needed
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }).start();
    }
}