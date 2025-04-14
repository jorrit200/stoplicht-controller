package com.stoplicht_controller.stoplicht_controller.Models;

import com.stoplicht_controller.stoplicht_controller.Enums.LightState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Trafficlight {
    private int priority;
    private LightState lightState;
    private int ms;

    public Trafficlight(LightState lightState) {
    }
    public Trafficlight(LightState lightState, int ms) {
    }
}
