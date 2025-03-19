package com.stoplicht_controller.stoplicht_controller.Services;

import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqPublisher;
import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqSubscriber;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrafficlightController {
    @Autowired
    private ZmqSubscriber zmqSubscriber;

    @Autowired
    private ZmqPublisher zmqPublisher;

    public TrafficlightController() {}

    public void start() {
        while(true){

            val tijd = zmqSubscriber.receiveMessage("tijd");
            val voorrangsvoertuig = zmqSubscriber.receiveMessage("voorrangsvoertuig");
            val sensorenRijbaan = zmqSubscriber.receiveMessage("sensoren_rijbaan");
            val sensorenSpeciaal = zmqSubscriber.receiveMessage("sensorenSpeciaal");

            /// Puntensysteem, intersectie punten aftrek?



            zmqPublisher.sendMessage("stoplichten", "tobeimplementedjsonformat" );
        }
    }



}
