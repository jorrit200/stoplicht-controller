package com.stoplicht_controller.stoplicht_controller.Entities;

import Config.TrafficLightConfig;
import com.stoplicht_controller.stoplicht_controller.Enums.TrafficlightState;
import com.stoplicht_controller.stoplicht_controller.Services.JsonReader;
import org.springframework.stereotype.Service;

import java.util.Dictionary;
import java.util.Hashtable;

@Service
public class TrafficLights {
    public Dictionary<Integer, TrafficlightState> trafficLights = new Hashtable<Integer, TrafficlightState>();

    public TrafficLights() {
        TrafficLightConfig trafficLightConfigFromSpec = JsonReader.getTrafficLightConfigFromSpec();
        trafficLightConfigFromSpec.getGroups().keySet()
                .forEach(groupKey -> trafficLights.put(groupKey, TrafficlightState.ROOD));
    }
}
