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

    public static SensorenRijbaan getSensorenRijbaanConfigFromSpec(){
        String url = "https://raw.githubusercontent.com/jorrit200/stoplicht-communicatie-spec/refs/heads/main/topics/sensoren_rijbaan/topic.json";
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(new URL(url), SensorenRijbaan.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SensorenSpeciaal GetSensorenSpeciaalConfigFromSpec(){
        String url = "https://raw.githubusercontent.com/jorrit200/stoplicht-communicatie-spec/refs/heads/main/topics/sensoren_speciaal/topic.json";
        ObjectMapper objectMapper = new ObjectMapper();

        try{
            return objectMapper.readValue(new URL(url), SensorenSpeciaal.class);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static Stoplicht GetStoplichtenConfigFromSpec(){
        String url = "https://raw.githubusercontent.com/jorrit200/stoplicht-communicatie-spec/refs/heads/main/topics/stoplichten/topic.json";
        ObjectMapper objectMapper = new ObjectMapper();

        try{
            return objectMapper.readValue(new URL(url), Stoplicht.class);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static Tijd GetTijdFromSpec(){
        String url = "https://raw.githubusercontent.com/jorrit200/stoplicht-communicatie-spec/refs/heads/main/topics/tijd/topic.json";
        ObjectMapper objectMapper = new ObjectMapper();

        try{
            return objectMapper.readValue(new URL(url), Tijd.class);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static VoorrangsvoertuigRij GetVoorrangsvoertuigRijFromSpec(){
        String url = "https://raw.githubusercontent.com/jorrit200/stoplicht-communicatie-spec/refs/heads/main/topics/voorrangsvoertuig/topic.json";
        ObjectMapper objectMapper = new ObjectMapper();

        try{
            return objectMapper.readValue(new URL(url), VoorrangsvoertuigRij.class);
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

}
