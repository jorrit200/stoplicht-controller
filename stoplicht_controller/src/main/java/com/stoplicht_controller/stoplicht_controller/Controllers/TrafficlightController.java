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

                // Start
                executeTrafficCycle(time, priorityVehicleQueue, sensorLane, sensorSpecial);

                // Send trafficlight
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

        // TODO: Volledig omgooien, troep
        for (Integer groupkey : intersectionData.getGroups().keySet()) {
            // Get active group in the for loop
            var group = intersectionData.getGroups().get(groupkey);

            // Check whether this group has any active conflicts
            var conflict = hasConflict(group);

            // Check whether transition to green light is possible
            boolean requirementsMet = transitionAllowed(group, sensorSpecial, sensorLane);

            // If there are no conflicts and the transition doesn't cause issues, set light to green.
            if (!conflict && requirementsMet) {
                updateTrafficLightState(groupkey.toString(), LightState.groen, time);
            }
            // If there are conflicts or a non-allowed transition,
            else {
                updateTrafficLightState(groupkey.toString(), LightState.oranje, time);
            }
        }
    }

    // TODO: Deze functie veranderen zodat hij ambulance (prio 1) en bussen (prio 2) anders afhandelt
    // prio 1: eerst volgende traffic cycle groen voor deze vehicle
    // prio 2: hoger gewicht voor punten systeem
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

    // Check whether any of the intersections of given group have a green light.
    // Returns true if it has a intersections
    private boolean hasConflict(IntersectionData.Group group) {
        // Get alle intersecting with given group
        return group.getIntersectsWith()
                // Loop through intersecting groups
                .stream()
                // Map the integers to string because key values are string
                .map(Object::toString)
                // Check if any of the conflict groups have green lights
                .anyMatch(conflictGroup ->
                        trafficLights.getStoplichten()
                                .get(conflictGroup)
                                .stream()
                                .findFirst()
                                .map(Trafficlight::getLightState)
                                .equals(LightState.groen));
    }

    //
    private boolean transitionAllowed(IntersectionData.Group group, SensorSpecial sensorSpecial, SensorLane sensorLane) {
        // Als er geen transitie vereisten zijn, is de overgang toegestaan
        if (group.getTransitionRequirements() == null) return true;

        // Controleer alle transitie vereisten
        return group.getTransitionRequirements()
                // Get requirements before a green light is allowed
                .getGreen()
                // Loop through list of requirements for green
                .stream()
                .allMatch(req -> {
                    // Check if type of requirement is equal to type "sensor"
                    if ("sensor".equals(req.getType())) {
                        // Fetch the value of desired sensor
                        boolean sensorValue = getSpecialSensorValue(req.getSensor(), sensorSpecial);
                        // Check if the sensor has the desired value
                        return sensorValue == req.getSensorState();
                    }

                    // If requirement type is not of type "sensor" allow it anyway
                    // Issue: Might need to handle different types of types later, invalidating this return
                    return true;
                });
    }

    // sensorName = type of special sensor
    // sensorSpecial = {"brug_wegdek":true,"brug_water":false,"brug_file":true} from topic
    // Get value of special sensor from topic
    public boolean getSpecialSensorValue(String sensorName, SensorSpecial sensorSpecial) {
        return switch (sensorName) {
            case "brug_wegdek" -> sensorSpecial.isBridge_road();
            case "brug_water" -> sensorSpecial.isBridge_water();
            case "brug_file" -> sensorSpecial.isBridge_traffic();
            default -> throw new IllegalArgumentException("Sensor naam is invalide: [" + sensorName + "] Controleer de naam met de spec.");
        };
    }

    // TODO: Rework
    private void updateTrafficLightState(String groupKey, LightState lightState, Time time) {
//        Trafficlight currentTrafficlight = trafficLights.getStoplichten().get(groupKey);
//
//        if (currentTrafficlight != null
//                && currentTrafficlight.getLightState() == LightState.oranje
//                && (time.getMs() - currentTrafficlight.getMs()) >= 3500) {
//            trafficLights.getStoplichten().put(groupKey, new Trafficlight(LightState.rood, time.getMs()));
//        }
    }

    // Note: Misschien rework?
    //ja dit is beun
    private void sendTrafficLightsToPublisher() throws JsonProcessingException {
        Map<String, String> simplifiedMap = new HashMap<>();

//        trafficLights.getStoplichten().elements().asIterator().forEachRemaining(tl -> {
//            simplifiedMap.put(tl.getLightId(), tl.getLightState().toString());
//        });

        String json = objectMapper.writeValueAsString(simplifiedMap);

        System.out.println("Sent: [" + "stoplichten" + "] " + json);

        zmqPublisher.sendMessage("stoplichten", json);
    }
}
