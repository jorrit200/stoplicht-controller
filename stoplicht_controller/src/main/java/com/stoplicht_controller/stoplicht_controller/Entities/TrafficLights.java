package com.stoplicht_controller.stoplicht_controller.Entities;

import Config.TrafficLightConfig;
import com.stoplicht_controller.stoplicht_controller.Enums.TrafficlightState;
import com.stoplicht_controller.stoplicht_controller.Util.JsonReader;
import org.apache.catalina.webresources.StandardRoot;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;
import java.util.Dictionary;
import java.util.Hashtable;

@Service
public class TrafficLights {
    public Dictionary<Integer, TrafficlightState> trafficLights = new Hashtable<Integer, TrafficlightState>();

    public TrafficLights() {
        TrafficLightConfig trafficLightConfigFromSpec = JsonReader.getTrafficLightConfigFromSpec();
        trafficLightConfigFromSpec.getGroups().keySet()
                .forEach(groupKey -> trafficLights.put(groupKey, TrafficlightState.ROOD));
    }

    public String fillTrafficLights() throws JSONException {
        try{
            var test =  trafficLights.put(1, TrafficlightState.GROEN);
            JSONObject trafficLightsJSON = new JSONObject(String.valueOf(test));

            return trafficLightsJSON.toString();

        } catch (JSONException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while filling traffic lights", e);
        }

    }

}
