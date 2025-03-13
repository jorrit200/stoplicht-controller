package com.stoplicht_controller.stoplicht_controller.Services;

import Config.TrafficLightConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;

@Service
public class JsonReader {
    public static TrafficLightConfig getTrafficLightConfigFromSpec () {
        String url = "https://raw.githubusercontent.com/jorrit200/stoplicht-communicatie-spec/main/intersectionData/lanes.json";
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(new URL(url), TrafficLightConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
