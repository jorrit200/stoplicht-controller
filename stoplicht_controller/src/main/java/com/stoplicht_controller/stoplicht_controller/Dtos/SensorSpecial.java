package com.stoplicht_controller.stoplicht_controller.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorSpecial {
    @JsonProperty("brug_wegdek")
    private boolean bridge_road_value = false;
    @JsonProperty("brug_water")
    private boolean bridge_water_value = false;
    @JsonProperty("brug_file")
    private boolean bridge_traffic_value = false;
}
