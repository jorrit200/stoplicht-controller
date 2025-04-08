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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IntersectionData intersectionData = JsonReader.getTrafficLightConfigFromSpec();

    @Autowired private JsonMessageReceiver jsonMessageReceiver;
    @Autowired private ZmqPublisher zmqPublisher;
    @Autowired private TrafficlightData trafficLights;



    /// Orange implementeren, volgens nederlandse wet 3.5 seconden
    /// Cycle implementeren met puntensysteem
    /// Tijd implementeren (in ms)
    public void start() {
        while (true) {
            try {
                //Topics
                SensorLane sensorLane = jsonMessageReceiver.receiveMessage("sensoren_rijbaan", SensorLane.class);
                Time time = jsonMessageReceiver.receiveMessage("tijd", Time.class);
                PriorityVehicleQueue priorityVehicleQueue = jsonMessageReceiver.receiveMessage("voorrangsvoertuig", PriorityVehicleQueue.class);
                SensorSpecial sensorSpecial = jsonMessageReceiver.receiveMessage("sensoren_speciaal", SensorSpecial.class);

                //Cycles
                if (!priorityVehicleQueue.getQueue().isEmpty()) {
                    processPriorityVehicle(priorityVehicleQueue);
                }

                executeTrafficCycle(time, priorityVehicleQueue, sensorLane, sensorSpecial);

                //Send trafficlight
                sendTrafficLightsToPublisher();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void executeTrafficCycle(Time time, PriorityVehicleQueue priorityVehicleQueue, SensorLane sensorLane, SensorSpecial sensorSpecial) {
        for (Integer groupkey : intersectionData.getGroups().keySet()) {

            var group = intersectionData.getGroups().get(groupkey);
            var conflict = hasConflict(group);
            boolean requirementsMet = transitionAllowed(group, sensorSpecial, sensorLane, priorityVehicleQueue);

            if (!conflict && requirementsMet) {
                updateTrafficLightState(groupkey.toString(), LightState.groen);

            } else {
                updateTrafficLightState(groupkey.toString(), LightState.rood);
            }
        }
    }

    private boolean transitionAllowed(IntersectionData.Group group, SensorSpecial sensorSpecial, SensorLane sensorLane, PriorityVehicleQueue priorityVehicleQueue) {
        // Als er geen transitievereisten zijn, is de overgang toegestaan
        if (group.getTransitionRequirements() == null) return true;

        // Controleer alle vereisten met Streams
        return group.getTransitionRequirements().
                getGreen()
                .stream()
                .allMatch(req -> {
                    if ("sensor".equals(req.getType())) {
                        boolean sensorValue = getSensorValue(req.getSensor(), sensorSpecial, sensorLane);
                        return sensorValue == req.getSensorState();
                    }
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
            String groupKey = voertuig.getLane();
            if (groupKey != null) {
                var group = intersectionData.getGroups().get(groupKey);
                var conflict = hasConflict(group);

                if (!conflict) {
                    updateTrafficLightState(groupKey, LightState.groen);
                }
            }
        }
        sendTrafficLightsToPublisher();
    }

    private boolean hasConflict(IntersectionData.Group group) {
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

    private void sendTrafficLightsToPublisher() throws JsonProcessingException {
        Map<String, LightState> trafficLightsMap = new HashMap<>();

        Enumeration<String> keys = trafficLights.getStoplichten().keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            trafficLightsMap.put(key, trafficLights.getStoplichten().get(key).getLightState());
        }

        String json = objectMapper.writeValueAsString(trafficLightsMap);
        zmqPublisher.sendMessage("stoplichten", json);
    }
}
