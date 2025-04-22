package com.stoplicht_controller.stoplicht_controller.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stoplicht_controller.stoplicht_controller.Enums.LightState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Trafficlight {
    @JsonIgnore
    private int weight;
    @JsonIgnore
    private int baseWeight;
    private LightState lightState;
    private String LightId;
    @JsonIgnore
    private int orange_started_ms;

    public Trafficlight(){}

    public Trafficlight(LightState lightState, int orange_started_ms) {
    }

    public int getWeight() {
        return weight + baseWeight;
    }

    public void addWeight(int weight) {
        this.weight += weight;
    }
}
