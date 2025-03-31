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
            trafficlight.trafficLights.put("1.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("2.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("2.2", TrafficlightState.groen);
            trafficlight.trafficLights.put("3.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("4.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("5.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("6.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("7.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("8.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("8.2", TrafficlightState.groen);
            trafficlight.trafficLights.put("9.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("10.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("11.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("12.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("21.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("22.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("24.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("25.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("26.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("27.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("27.2", TrafficlightState.groen);
            trafficlight.trafficLights.put("28.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("31.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("31.2", TrafficlightState.groen);
            trafficlight.trafficLights.put("32.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("32.2", TrafficlightState.groen);
            trafficlight.trafficLights.put("33.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("33.2", TrafficlightState.groen);
            trafficlight.trafficLights.put("34.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("34.2", TrafficlightState.groen);
            trafficlight.trafficLights.put("35.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("35.2", TrafficlightState.groen);
            trafficlight.trafficLights.put("36", TrafficlightState.groen);
            trafficlight.trafficLights.put("36.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("37.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("37.2", TrafficlightState.groen);
            trafficlight.trafficLights.put("38.1", TrafficlightState.groen);
            trafficlight.trafficLights.put("38.2", TrafficlightState.groen);

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(trafficlight.trafficLights); // Serialiseer de hele map
            System.out.println(json);
            return json;

        } catch (Exception e) {
            throw new RuntimeException("An error occurred while filling traffic lights", e);
        }
    }

}
