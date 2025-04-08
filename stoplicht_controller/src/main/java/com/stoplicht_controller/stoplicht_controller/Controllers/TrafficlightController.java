package com.stoplicht_controller.stoplicht_controller.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqPublisher;
import com.stoplicht_controller.stoplicht_controller.Dtos.PriorityVehicleQueue;
import com.stoplicht_controller.stoplicht_controller.Dtos.SensorLane;
import com.stoplicht_controller.stoplicht_controller.Dtos.SensorSpecial;
import com.stoplicht_controller.stoplicht_controller.Dtos.Time;
import com.stoplicht_controller.stoplicht_controller.Enums.LightState;
import com.stoplicht_controller.stoplicht_controller.Models.IntersectionData;
import com.stoplicht_controller.stoplicht_controller.Models.Trafficlight;
import com.stoplicht_controller.stoplicht_controller.Models.TrafficlightData;
import com.stoplicht_controller.stoplicht_controller.Util.JsonReader;
import com.stoplicht_controller.stoplicht_controller.messaging.JsonMessageReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
public class TrafficlightController {
    ObjectMapper objectMapper = new ObjectMapper();
    IntersectionData intersectionData = JsonReader.getTrafficLightConfigFromSpec();
    @Autowired
    private JsonMessageReceiver jsonMessageReceiver;
    @Autowired
    private ZmqPublisher zmqPublisher;
    @Autowired
    private TrafficlightData trafficLights;

    public TrafficlightController() {
    }

    /// Orange implementeren, volgens nederlandse wet 3.5 seconden
    /// Cycle implementeren met puntensysteem
    /// Tijd implementeren (in ms)
    public void start() {
        while (true) {
            try {
                //these might all break due to namechange Dutch -> English
                SensorLane sensorLane = jsonMessageReceiver.receiveMessage("sensoren_rijbaan", SensorLane.class);
                Time time = jsonMessageReceiver.receiveMessage("tijd", Time.class);
                PriorityVehicleQueue priorityVehicleQueue = jsonMessageReceiver.receiveMessage("voorrangsvoertuig", PriorityVehicleQueue.class);
                SensorSpecial sensorSpecial = jsonMessageReceiver.receiveMessage("sensoren_speciaal", SensorSpecial.class);

                if (!priorityVehicleQueue.getQueue().isEmpty()) {
                    processPriorityVehicle(priorityVehicleQueue);
                }

                //Cycles
                startCycle(time, priorityVehicleQueue, sensorLane, sensorSpecial);
                String trafficLightsJson = objectMapper.writeValueAsString(trafficLights);
                zmqPublisher.sendMessage("stoplichten", trafficLightsJson);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void startCycle(Time time, PriorityVehicleQueue priorityVehicleQueue, SensorLane sensorLane, SensorSpecial sensorSpecial) {
        for (Integer groupkey : intersectionData.getGroups().keySet()) {
            var group = intersectionData.getGroups().get(groupkey);

            // 1. Conflictdetectie met Streams: Controleer of een conflicterende groep al op groen staat
            var conflict = hasConflict(group, trafficLights);

            // 2. Controleer of de transitievereisten zijn voldaan
            boolean requirementsMet = checkTransitionRequirements(group, sensorSpecial, sensorLane, priorityVehicleQueue);

            // 3. Als er geen conflicten zijn en de transitievereisten zijn voldaan, zet het verkeerslicht op groen
            if (!conflict && requirementsMet) {
                updateTrafficLightState(groupkey.toString(), LightState.groen);

            } else {
                updateTrafficLightState(groupkey.toString(), LightState.rood);
            }
        }
    }

    private boolean checkTransitionRequirements(IntersectionData.Group group, SensorSpecial sensorSpecial, SensorLane sensorLane, PriorityVehicleQueue priorityVehicleQueue) {
        // Als er geen transitievereisten zijn, is de overgang toegestaan
        if (group.getTransitionRequirements() == null)
            return true;

        // Controleer alle vereisten met Streams
        return group.getTransitionRequirements().getGreen().stream().allMatch(req -> {
            if ("sensor".equals(req.getType())) {
                boolean sensorValue = getSensorValue(req.getSensor(), sensorSpecial, sensorLane);
                return sensorValue == req.getSensorState();
            }
            // Voeg hier extra voorwaarden toe indien nodig
            return true;
        });
    }

    public boolean getSensorValue(String sensorName, SensorSpecial sensorSpecial, SensorLane sensorLane) {
        switch (sensorName) {
            case "bridge_road":
                return sensorSpecial.isBridge_road();
            case "bridge_water":
                return sensorSpecial.isBridge_water();
            case "bridge_traffic":
                return sensorSpecial.isBridge_traffic();
            default:
                SensorLane.SensorStatus sensorStatus = sensorLane.getSensors().get(sensorName);
                if (sensorStatus != null) {
                    return sensorStatus.isFront();
                } else {
                    throw new IllegalArgumentException("Sensor niet gevonden: " + sensorName);
                }
        }
    }

    public void processPriorityVehicle(PriorityVehicleQueue priorityVehicleQueue) throws JsonProcessingException {

        for (PriorityVehicleQueue.PriorityVehicle voertuig : priorityVehicleQueue.getQueue()) {
            String laneId = voertuig.getLane();
            //get groupKey
            String groupKey = laneId;
            if (groupKey != null) {
                var group = intersectionData.getGroups().get(groupKey);
                var conflict = group.getIntersectsWith().stream()
                        .anyMatch(conflictGroup -> trafficLights.getStoplichten().get(conflictGroup).getLightState() == LightState.groen);

                if (!conflict) {
                    updateTrafficLightState(groupKey, LightState.groen);
                }
            }
        }

        zmqPublisher.sendMessage("stoplichten", trafficLightsJson(trafficLights.getStoplichten()));
        start();
    }

    //todo change dictionary to String and String
    private String trafficLightsJson(Dictionary<String, Trafficlight> trafficLights) throws JsonProcessingException {
        Map<String, LightState> trafficLightsMap = new HashMap<>();

        Enumeration<String> keys = trafficLights.keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement(); // Key
            trafficLightsMap.put(key, trafficLights.get(key).getLightState()); // Only use LightState
        }

        return objectMapper.writeValueAsString(trafficLightsMap);
    }
    private boolean hasConflict(IntersectionData.Group group, TrafficlightData trafficLights) {
        return group.getIntersectsWith().stream()
                .map(Object::toString)
                .anyMatch(conflictGroup ->
                        trafficLights.getStoplichten()
                                .get(conflictGroup)
                                .getLightState() == LightState.groen);
    }

    private void updateTrafficLightState(String groupKey, LightState lightState) {
        trafficLights.getStoplichten().put(groupKey, new Trafficlight(lightState));
    }
}
