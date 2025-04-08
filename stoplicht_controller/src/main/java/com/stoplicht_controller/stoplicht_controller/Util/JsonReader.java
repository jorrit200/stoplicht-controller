package com.stoplicht_controller.stoplicht_controller.Util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Models.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URL;

@Component
public class JsonReader {
    public static IntersectionData getTrafficLightConfigFromSpec () {
        String url = "https://raw.githubusercontent.com/jorrit200/stoplicht-communicatie-spec/main/intersectionData/lanes.json";
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(new URL(url), IntersectionData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
