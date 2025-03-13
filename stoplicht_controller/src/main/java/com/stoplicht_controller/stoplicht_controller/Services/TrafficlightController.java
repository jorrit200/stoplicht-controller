package com.stoplicht_controller.stoplicht_controller.Services;

import com.stoplicht_controller.stoplicht_controller.Util.ZmqSubscriber;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrafficlightController {
    @Autowired
    private ZmqSubscriber zmqSubscriber;

    public TrafficlightController() {}

    public void start() {
        val message = zmqSubscriber.receiveMessage("trafficlightstatus");


    }
}
