package com.stoplicht_controller.stoplicht_controller.Dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorLane {
    public Map<String, SensorStatus> sensors;

    @Getter
    @Setter
    public class SensorStatus {
        @JsonProperty("voor")
        private boolean front;
        @JsonProperty("achter")
        private boolean back;
    }
}
