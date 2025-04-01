package com.stoplicht_controller.stoplicht_controller.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Models.Stoplicht;
import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqPublisher;
import com.stoplicht_controller.stoplicht_controller.Enums.LightState;
import com.stoplicht_controller.stoplicht_controller.Models.TrafficLight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Service;

import java.util.Dictionary;
import java.util.Hashtable;


//this entire class can be deleted once the controller runs properly && simulation
@Service
public class TrafficlightStatePublisher {

    @Autowired
    private ZmqPublisher publisher;
    @Autowired
    private Stoplicht trafficLights;

    public TrafficlightStatePublisher(ZmqPublisher publisher, Stoplicht trafficLights) {
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


    public String fillTrafficLights(Stoplicht trafficlight) throws JSONException {
        try {
            Dictionary<String, TrafficLight> result =  new Hashtable<String, TrafficLight>();
            result.put("1.1", new TrafficLight(LightState.groen));
            result.put("2.1", new TrafficLight(LightState.groen));
            result.put("3.1", new TrafficLight(LightState.groen));
            result.put("4.1", new TrafficLight(LightState.groen));
            result.put("5.1", new TrafficLight(LightState.groen));

            trafficlight.setStoplichten(result);

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(trafficlight.getStoplichten()); // Serialiseer de hele map
            System.out.println(json);
            return json;

        } catch (Exception e) {
            throw new RuntimeException("An error occurred while filling traffic lights", e);
        }
    }

}
