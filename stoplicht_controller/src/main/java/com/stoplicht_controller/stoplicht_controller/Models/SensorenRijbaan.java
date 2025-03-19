package com.stoplicht_controller.stoplicht_controller.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.Map;


@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorenRijbaan {
    private Map<String, SensorStatus> sensors;

    @Getter
    public class SensorStatus {
        private boolean voor;
        private boolean achter;
    }
}
