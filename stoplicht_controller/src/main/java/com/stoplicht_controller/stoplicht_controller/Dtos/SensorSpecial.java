package com.stoplicht_controller.stoplicht_controller.Dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorSpecial {
    private boolean bridge_road;
    private boolean bridge_water;
    private boolean bridge_traffic;
}
