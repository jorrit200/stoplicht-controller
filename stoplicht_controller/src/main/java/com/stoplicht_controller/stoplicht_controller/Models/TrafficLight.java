package com.stoplicht_controller.stoplicht_controller.Models;

import com.stoplicht_controller.stoplicht_controller.Enums.LightState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrafficLight {
    private int priority;
    private LightState lightState;

    public TrafficLight(LightState lightState) {
    }
}
