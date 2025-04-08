package com.stoplicht_controller.stoplicht_controller.Dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
        private boolean voor;
        private boolean achter;
    }
}
