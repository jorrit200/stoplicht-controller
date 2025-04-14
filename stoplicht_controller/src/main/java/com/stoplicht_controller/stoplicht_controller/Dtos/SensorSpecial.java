package com.stoplicht_controller.stoplicht_controller.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorSpecial {
    @JsonProperty("brug_wegdek")
    private boolean bridge_road;
    @JsonProperty("brug_water")
    private boolean bridge_water;
    @JsonProperty("brug_file")
    private boolean bridge_traffic;
}
