package com.stoplicht_controller.stoplicht_controller;

import com.stoplicht_controller.stoplicht_controller.Configurations.TestPublisher;
import com.stoplicht_controller.stoplicht_controller.Controllers.TrafficlightController;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class StoplichtControllerApplication {
    @Autowired
    private TrafficlightController trafficlightController;
//    @Autowired
//    private TestPublisher testPublisher;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public static void main(String[] args) {
        SpringApplication.run(StoplichtControllerApplication.class, args);
    }

    @PostConstruct
    public void init() {
        executor.submit(() -> trafficlightController.start());
//        executor.submit(() -> {
//            try {
//                testPublisher.startLoop();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
    }
}