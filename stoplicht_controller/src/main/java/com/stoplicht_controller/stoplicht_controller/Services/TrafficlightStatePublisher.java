package com.stoplicht_controller.stoplicht_controller.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Models.TrafficLights;
import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqPublisher;
import com.stoplicht_controller.stoplicht_controller.Enums.TrafficlightState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
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
        try {
            publisher.sendMessage("stoplichten", fillTrafficLights(trafficLights));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    public String fillTrafficLights(TrafficLights trafficlight) throws JSONException {
        try {
            trafficlight.trafficLights.put("1.1", TrafficlightState.groen); // Voeg de waarde toe

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(trafficlight.trafficLights); // Serialiseer de hele map
            System.out.println(json);
            return json;

        } catch (Exception e) {
            throw new RuntimeException("An error occurred while filling traffic lights", e);
        }
    }

}
