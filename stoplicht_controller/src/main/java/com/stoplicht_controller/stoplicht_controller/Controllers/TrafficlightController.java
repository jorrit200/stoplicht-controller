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

import java.util.*;

@Service
public class TrafficlightController {

    @Autowired
    private TrafficlightData trafficLights;

    public TrafficlightController() {}
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IntersectionData intersectionData = JsonReader.getTrafficLightConfigFromSpec();

    @Autowired private JsonMessageReceiver jsonMessageReceiver;
    @Autowired private ZmqPublisher zmqPublisher;

    private int simulation_time_last_updated_ms;
    private SensorLane current_lane_state;

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

                if (current_lane_state == null)
                    current_lane_state = sensorLane;

                trafficCycle(sensorLane, time, priorityVehicleQueue, sensorSpecial);

//                if (priorityVehicleQueue == null || priorityVehicleQueue.getQueue() == null){
//                    priorityVehicleQueue.setQueue(new ArrayList<>());
//                }
//
//                // Start
//                executeTrafficCycle(time, priorityVehicleQueue, sensorLane, sensorSpecial);
//
//                // Send trafficlight
//                sendTrafficLightsToPublisher();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void trafficCycle(SensorLane sensorLane, Time time,
                             PriorityVehicleQueue priorityVehicleQueue, SensorSpecial sensorSpecial)
    {
        // If there is a priority queue
        if (priorityVehicleQueue != null) {
            // Ensure queue is sorted by lowest simulation time first
            priorityVehicleQueue.sortQueueBySimulationTime();

            boolean hadAmbulance = false;

            // For each priority vehicle in the queue
            for (PriorityVehicleQueue.PriorityVehicle vehicle : priorityVehicleQueue.getQueue()) {
                // Switch functionality depending on priority
                switch (vehicle.getPriority()) {
                    case 1: { // Ambulance
                        if (!hadAmbulance) {
                            // Set hadAmbulance to true, so we don't crash ambulances
                            hadAmbulance = true;

                            // Add weight to ambulance lane, to ensure it will always go green on next cycle
                            AddWeightToLane(vehicle.getLane(), 5000);
                        }
                    }
                    case 2: { // Bus
                        AddWeightToLane(vehicle.getLane(), 420);
                    }
                }
            }

            // Force next light cycle
            if (hadAmbulance && time != null) {
                int time_difference = time.getMs() - simulation_time_last_updated_ms;
                simulation_time_last_updated_ms += 7000 - time_difference;
            }
        }

        // Check if time != null
        if (time != null) {
            // Switch green lights to orange
            if (time.getMs() - simulation_time_last_updated_ms >= 7000 ||
                    time.getMs() - simulation_time_last_updated_ms < 10500) {
                ChangeTrafficLights(LightState.oranje);
            }

            // Switch orange to red
            if (time.getMs() - simulation_time_last_updated_ms >= 10500) {
                ChangeTrafficLights(LightState.rood);

                // Logic for green lights
            }
        }
    }

    public void AddWeightToLane(String lane, int weight) {
        String laneKey = lane.split(".")[0];

        List<Trafficlight> laneGroup = trafficLights.getStoplichten().get(laneKey);

        for (Trafficlight light : laneGroup) {
            light.addWeight(weight);
        }
    }

    public void ChangeTrafficLights(LightState change_light_to) {
        // Enumeration for traffic lights
        Enumeration<String> keys = trafficLights.getStoplichten().keys();

        while (keys.hasMoreElements()) {
            String groupKey = keys.nextElement();
            List<Trafficlight> trafficLightsFromGroup = trafficLights.getStoplichten().get(groupKey);

            for (Trafficlight light : trafficLightsFromGroup) {
                switch (change_light_to) {
                    // If light is red, change to orange
                    case oranje:
                        if (light.getLightState() == LightState.groen)
                            light.setLightState(LightState.oranje);
                        break;
                    // If light is orange, change to red
                    case rood:
                        if (light.getLightState() == LightState.oranje)
                            light.setLightState(LightState.rood);
                        break;
                }
            }
        }
    }

    public void GenerateGreenLightCombination(SensorLane sensorLane) {
        // Check whether SensorLane had sensor changes
        for (String key : sensorLane.sensors.keySet()) {
            var old_state = sensorLane.sensors.get(key);
            var new_state = current_lane_state.sensors.get(key);

            // If back sensor became true, add 2 weight
            if (!old_state.isBack() && new_state.isBack())
                AddWeightToLane(key, 2);

            // if front sensor became true, add 1 weight
            if (!old_state.isFront() && new_state.isFront())
                AddWeightToLane(key, 1);
        }

        // Update lane state tracked my controller
        current_lane_state = sensorLane;

        // Create a list that orders groups by the sum of the lane weight
        Map<String, Integer> group_weight = new HashMap<>();

        Enumeration<String> keys = trafficLights.getStoplichten().keys();

        while (keys.hasMoreElements()) {
            String group_key = keys.nextElement();
            List<Trafficlight> lights = trafficLights.getStoplichten().get(group_key);

            int weight_sum = 0;
            for (Trafficlight light : lights) {
                weight_sum += light.getWeight();
            }

            group_weight.put(group_key, weight_sum);
        }

        // Sort the group_weight descending
        List<Map.Entry<String, Integer>> sorted_group_weight = new ArrayList<>(group_weight.entrySet());
        sorted_group_weight.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

        List<Trafficlight> heaviest_group = trafficLights.getStoplichten()
                .get(sorted_group_weight.getFirst()
                        .getKey());

        // Set heaviest group lights to green
        for (Trafficlight light : heaviest_group)
        {
            light.setLightState(LightState.groen);
        }

        // Set other possible groups to green
        for (Integer group_key : intersectionData.getGroups().keySet()) {
            // Get active group
            var group = intersectionData.getGroups().get(group_key);

            boolean group_has_conflict = hasGroupConflicts(group);
        }
    }

    public boolean hasGroupConflicts (IntersectionData.Group group) {
        List<Integer> group_intersects_with = group.getIntersectsWith();
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
