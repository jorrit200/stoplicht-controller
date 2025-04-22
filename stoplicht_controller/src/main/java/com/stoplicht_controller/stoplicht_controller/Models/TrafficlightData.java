package com.stoplicht_controller.stoplicht_controller.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stoplicht_controller.stoplicht_controller.Enums.LightState;
import com.stoplicht_controller.stoplicht_controller.Util.JsonReader;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.*;

@Getter
@Setter
@Component
public class TrafficlightData {
    private Dictionary<String, List<Trafficlight>> stoplichten = new Hashtable<>();
    @JsonIgnore
    private final IntersectionData intersectionData = JsonReader.getTrafficLightConfigFromSpec();

    public TrafficlightData() {
        // Loop through groups
        intersectionData.getGroups().forEach((groupKey, group) -> {
            // Get lanes inside a group
            Map<String, IntersectionData.Lane> lanes = group.getLanes();
            // Create new list for traffic lights inside a group
            List<Trafficlight> trafficLights = new ArrayList<>();

            // Loop through each lane in a group
            lanes.forEach((laneKey, lane) -> {
                // Create traffic lights with default values
                Trafficlight trafficlight = new Trafficlight();
                // Default values
                trafficlight.setWeight(0);
                trafficlight.setLightState(LightState.rood);

                // If roads are central ring, give it a higher base weight
                if (groupKey == 2 || groupKey == 8)
                {
                    trafficlight.setBaseWeight(1);
                    trafficlight.setLightState(LightState.rood);
                }
                else
                    trafficlight.setBaseWeight(0);


                trafficlight.setLightId(groupKey.toString() + "." + laneKey);
                trafficlight.setOrange_started_ms(0);

                trafficLights.add(trafficlight);
            });

            // Put the list in dictionary connecting lights to a group
            stoplichten.put(groupKey.toString(), trafficLights);
        });
    }
}
