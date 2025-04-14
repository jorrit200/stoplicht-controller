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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

@Service
public class TrafficlightController {

    @Autowired
    private TrafficlightData trafficLights;

    public TrafficlightController() {}
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IntersectionData intersectionData = JsonReader.getTrafficLightConfigFromSpec();

    @Autowired private JsonMessageReceiver jsonMessageReceiver;
    @Autowired private ZmqPublisher zmqPublisher;

    private PriorityVehicleQueue priorityVehicleQueue = new PriorityVehicleQueue();

    /// Orange implementeren, volgens nederlandse wet 3.5 seconden
    /// Cycle implementeren met puntensysteem
    /// Tijd implementeren (in ms)
    public void start() {
        while (true) {
            try {
                //Topics
                SensorLane sensorLane = jsonMessageReceiver.receiveMessage("sensoren_rijbaan", SensorLane.class);
                Time time = jsonMessageReceiver.receiveMessage("tijd", Time.class);
                priorityVehicleQueue = jsonMessageReceiver.receiveMessage("voorrangsvoertuig", PriorityVehicleQueue.class);
                SensorSpecial sensorSpecial = jsonMessageReceiver.receiveMessage("sensoren_speciaal", SensorSpecial.class);

                if (priorityVehicleQueue == null || priorityVehicleQueue.getQueue() == null){
                    priorityVehicleQueue.setQueue(new ArrayList<>());
                }

                //Start
                executeTrafficCycle(time, priorityVehicleQueue, sensorLane, sensorSpecial);

                //Send trafficlight
                sendTrafficLightsToPublisher();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void executeTrafficCycle(Time time, PriorityVehicleQueue priorityVehicleQueue, SensorLane sensorLane, SensorSpecial sensorSpecial) throws JsonProcessingException {
        if (!priorityVehicleQueue.getQueue().isEmpty()) {
            processPriorityVehicle(priorityVehicleQueue, time);
        }

        for (Integer groupkey : intersectionData.getGroups().keySet()) {

            var group = intersectionData.getGroups().get(groupkey);
            var conflict = hasConflict(group);
            boolean requirementsMet = transitionAllowed(group, sensorSpecial, sensorLane);

            if (!conflict && requirementsMet) {
                updateTrafficLightState(groupkey.toString(), LightState.groen, time);
            } else {
                updateTrafficLightState(groupkey.toString(), LightState.oranje, time);
            }
        }
    }

    private boolean transitionAllowed(IntersectionData.Group group, SensorSpecial sensorSpecial, SensorLane sensorLane) {
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
            case "brug_wegdek":
                return sensorSpecial.isBridge_road();
            case "brug_water":
                return sensorSpecial.isBridge_water();
            case "brug_file":
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

    public void processPriorityVehicle(PriorityVehicleQueue priorityVehicleQueue, Time time) throws JsonProcessingException {
        for (PriorityVehicleQueue.PriorityVehicle voertuig : priorityVehicleQueue.getQueue()) {
            var groupLane = voertuig.getLane();
            var groupKey = Integer.parseInt(groupLane.split("\\.")[0]);
            if (groupKey != 0) {
                var group = intersectionData.getGroups().get(groupKey);
                var conflict = hasConflict(group);

                if (!conflict) {
                    updateTrafficLightState(groupLane, LightState.groen, time);
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

    private void updateTrafficLightState(String groupKey, LightState lightState, Time time) {
        Trafficlight currentTrafficlight = trafficLights.getStoplichten().get(groupKey);

        if (currentTrafficlight != null
                && currentTrafficlight.getLightState() == LightState.oranje
                && (time.getMs() - currentTrafficlight.getMs()) >= 3500) {
            trafficLights.getStoplichten().put(groupKey, new Trafficlight(LightState.rood, time.getMs()));
        }
    }

    //ja dit is beun
    private void sendTrafficLightsToPublisher() throws JsonProcessingException {
        Map<String, String> simplifiedMap = new HashMap<>();

        trafficLights.getStoplichten().elements().asIterator().forEachRemaining(tl -> {
            simplifiedMap.put(tl.getLightId(), tl.getLightState().toString());
        });

        String json = objectMapper.writeValueAsString(simplifiedMap);

        System.out.println("Sent: [" + "stoplichten" + "] " + json);

        zmqPublisher.sendMessage("stoplichten", json);
    }
}
