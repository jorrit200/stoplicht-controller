package com.stoplicht_controller.stoplicht_controller.Entities;

import Config.TrafficLightConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Enums.TrafficlightState;
import com.stoplicht_controller.stoplicht_controller.Util.JsonReader;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Service;
import java.util.Dictionary;
import java.util.Hashtable;

@Service
public class TrafficLights {
    public Dictionary<String, TrafficlightState> trafficLights = new Hashtable<String, TrafficlightState>();

    public TrafficLights() {
        TrafficLightConfig trafficLightConfigFromSpec = JsonReader.getTrafficLightConfigFromSpec();
        trafficLightConfigFromSpec.getGroups().keySet()
                .forEach(groupKey -> trafficLights.put(groupKey.toString(), TrafficlightState.rood));
    }

    public String fillTrafficLights() throws JSONException {
        try {
            trafficLights.put("1.1", TrafficlightState.groen); // Voeg de waarde toe

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(trafficLights); // Serialiseer de hele map
            System.out.println(json);
            return json;

        } catch (Exception e) {
            throw new RuntimeException("An error occurred while filling traffic lights", e);
        }
    }

}
