package com.stoplicht_controller.stoplicht_controller.Models;

import com.stoplicht_controller.stoplicht_controller.Enums.TrafficlightState;
import com.stoplicht_controller.stoplicht_controller.Util.JsonReader;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Dictionary;
import java.util.Hashtable;

@Service
@Getter
public class TrafficLights {
    public Dictionary<String, TrafficlightState> trafficLights = new Hashtable<String, TrafficlightState>();

    public TrafficLights() {
//        IntersectionData intersectionDataFromSpec = JsonReader.getTrafficLightConfigFromSpec();
//        intersectionDataFromSpec.getGroups().keySet()
//                .forEach(groupKey -> trafficLights.put(groupKey.toString(), TrafficlightState.rood));
    }
}
