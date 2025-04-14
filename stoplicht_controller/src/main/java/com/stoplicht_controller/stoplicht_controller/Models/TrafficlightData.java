package com.stoplicht_controller.stoplicht_controller.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stoplicht_controller.stoplicht_controller.Enums.LightState;
import com.stoplicht_controller.stoplicht_controller.Util.JsonReader;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

@Getter
@Setter
@Component
public class TrafficlightData {
    private Dictionary<String, Trafficlight> stoplichten = new Hashtable<String, Trafficlight>();
    @JsonIgnore
    private final IntersectionData intersectionData = JsonReader.getTrafficLightConfigFromSpec();

    public TrafficlightData() {
        intersectionData.getGroups().forEach((groupKey, group) -> {
            Map<String, IntersectionData.Lane> lanes = group.getLanes();
            lanes.forEach((laneKey, lane) -> {
                Trafficlight tl = new Trafficlight();
                tl.setPriority(0);
                tl.setLightState(LightState.rood);
                tl.setLightId(groupKey.toString() + "." + laneKey);
                tl.setMs(0);

                stoplichten.put(groupKey.toString(), tl);
            });
        });
    }
}
