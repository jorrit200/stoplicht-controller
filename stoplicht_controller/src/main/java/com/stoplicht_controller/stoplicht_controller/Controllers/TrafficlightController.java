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
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.Console;
import java.util.*;

@Service
public class TrafficlightController {

    @Autowired
    private TrafficlightData trafficLights;

    public TrafficlightController() {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IntersectionData intersectionData = JsonReader.getTrafficLightConfigFromSpec();

    @Autowired
    private JsonMessageReceiver jsonMessageReceiver;
    @Autowired
    private ZmqPublisher zmqPublisher;

    private int simulation_time_last_updated_ms;

    private SensorLane sensorLane;
    private Time time;
    private PriorityVehicleQueue priorityVehicleQueue;
    private SensorSpecial sensorSpecial;

    private Boolean hasSentOrange = false;

    private Map<String, Class<?>> topicClassMap = Map.of(
            "sensoren_rijbaan", SensorLane.class,
            "sensoren_speciaal", SensorSpecial.class,
            "tijd", Time.class,
            "voorrangsvoertuig", PriorityVehicleQueue.class
    );

    /// Orange implementeren, volgens nederlandse wet 3.5 seconden
    /// Cycle implementeren met puntensysteem
    /// Tijd implementeren (in ms)
    public void start() {
        while (true) {
            try {
                String[] received = jsonMessageReceiver.receiveMessage();
                String topic = received[0];
                String json = received[1];

                Class<?> messageClass = topicClassMap.get(topic);

                if (messageClass == null) {
                    throw new IllegalArgumentException("Unknown topic: " + topic);
                }

                if (Objects.equals(topic, "sensoren_rijbaan")) {
                    json = "{\"sensors\":" + json + "}";
                }
                Object castedMessage = objectMapper.readValue(json, messageClass);

                if (castedMessage instanceof SensorLane) {
                    this.sensorLane = (SensorLane) castedMessage;
                } else if (castedMessage instanceof Time) {
                    this.time = (Time) castedMessage;
                } else if (castedMessage instanceof PriorityVehicleQueue) {
                    this.priorityVehicleQueue = (PriorityVehicleQueue) castedMessage;
                } else if (castedMessage instanceof SensorSpecial) {
                    this.sensorSpecial = (SensorSpecial) castedMessage;
                } else {
                    System.out.println("Unknown message type received: " + castedMessage.getClass());
                }

                //System.out.println("Received: [" + topic + "] Message:\n" + json);

                if (sensorLane != null && time != null && priorityVehicleQueue != null && sensorSpecial != null){
                    trafficCycle(sensorLane, time, priorityVehicleQueue, sensorSpecial);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void trafficCycle(SensorLane sensorLane, Time time,
                             PriorityVehicleQueue priorityVehicleQueue, SensorSpecial sensorSpecial) throws JsonProcessingException, JSONException {
        // If there is a priority queue
        if (priorityVehicleQueue.getQueue() != null) {
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
                            //AddWeightToLane(vehicle.getLane(), 5);
                        }
                    }
                    case 2: { // Bus
                        //AddWeightToLane(vehicle.getLane(), 2);
                    }
                }
            }

            //Force next light cycle
//            if (hadAmbulance && time != null) {
//                int time_difference = time.getMs() - simulation_time_last_updated_ms;
//                simulation_time_last_updated_ms += 7000 - time_difference;
//            }
        }

        // Check if time != null
        if (time != null) {
            // Switch orange to red
            if (time.getMs() - simulation_time_last_updated_ms >= 10500) {
                ChangeTrafficLights(LightState.rood);

                GenerateGreenLightCombination(sensorLane, sensorSpecial);
                simulation_time_last_updated_ms = time.getMs();
                hasSentOrange = false;

                sendTrafficLightsToPublisher();
            }

            // Switch green lights to orange
            if (time.getMs() - simulation_time_last_updated_ms >= 7000 &&
                    time.getMs() - simulation_time_last_updated_ms < 10500 && !hasSentOrange) {
                // TODO: Wrap this into an if, checking whether green lights are ALLOWED (by transition requirements) to go red
                ChangeTrafficLights(LightState.oranje);
                hasSentOrange = true;

                sendTrafficLightsToPublisher();
            }
        }
    }

    public void AddWeightToLane(String lane, int weight) {
        String laneKey = lane.split("\\.")[0];

        List<Trafficlight> laneGroup = trafficLights.getStoplichten().get(laneKey);

        laneGroup.get(0).addWeight(weight);
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

    public void GenerateGreenLightCombination(SensorLane sensorLane, SensorSpecial sensorSpecial) {
        // Check whether SensorLane had sensor changes
        for (String key : sensorLane.sensors.keySet()) {
            var old_state = sensorLane.sensors.get(key);
            var new_state = this.sensorLane.sensors.get(key);

            // If back sensor became true, add 2 weight
            if (!old_state.isBack() && new_state.isBack())
                AddWeightToLane(key, 1);

            // if front sensor became true, add 1 weight
            if (!old_state.isFront() && new_state.isFront())
                AddWeightToLane(key, 1);
        }

        // Update lane state tracked my controller
        this.sensorLane = sensorLane;

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
                .get(sorted_group_weight.get(0).getKey());

        // Set heaviest group lights to green
        for (Trafficlight light : heaviest_group) {
            light.setLightState(LightState.groen);
        }

        for (Map.Entry<String,Integer> group_with_weight : sorted_group_weight) {
            if (Objects.equals(group_with_weight.getKey(), sorted_group_weight.get(0).getKey())) continue;

            var group = intersectionData.getGroups().get(Integer.parseInt(group_with_weight.getKey()));

            boolean group_has_conflict = hasGroupConflicts(group);
            boolean transition_requirements_met = meetsTransitionRequirements(group_with_weight.getKey(), group, sensorSpecial);

            if (!group_has_conflict && transition_requirements_met) {
                List<Trafficlight> lights_in_group = trafficLights.getStoplichten().get(group_with_weight.getKey());

                for (Trafficlight tl : lights_in_group) {
                    tl.setLightState(LightState.groen);
                    // Reset light weight
//                    if (group_key == 2 || group_key == 8) {
//                        tl.setWeight(1);
//                    } else {
                    tl.setWeight(0);
//                    }
                }
            }
        }

        // Set other possible groups to green
//        for (Integer group_key : intersectionData.getGroups().keySet()) {
//            // Get active group
//            var group = intersectionData.getGroups().get(group_key);
//
//            boolean group_has_conflict = hasGroupConflicts(group);
//            boolean transition_requirements_met = meetsTransitionRequirements(group_key.toString(), group, sensorSpecial);
//
//            if (!group_has_conflict && transition_requirements_met) {
//                List<Trafficlight> lights_in_group = trafficLights.getStoplichten().get(group_key.toString());
//
//                for (Trafficlight tl : lights_in_group) {
//                    tl.setLightState(LightState.groen);
//                    // Reset light weight
////                    if (group_key == 2 || group_key == 8) {
////                        tl.setWeight(1);
////                    } else {
//                        tl.setWeight(0);
////                    }
//                }
//            }
//        }

        // Give remaining red lights extra weight
        keys = trafficLights.getStoplichten().keys();
        while (keys.hasMoreElements()) {
            String group_key = keys.nextElement();
            List<Trafficlight> lights = trafficLights.getStoplichten().get(group_key);

            // Add 1 weight to a light group if it remained red
            if (lights.get(0).getLightState() == LightState.rood)
                AddWeightToLane(group_key, 1);
        }
    }

    public boolean hasGroupConflicts(IntersectionData.Group group) {
        List<Integer> group_intersects_with = group.getIntersectsWith();

        // Check if any of the intersecting groups has a green light
        for (Integer group_key : group_intersects_with) {
            var traffic_lights = trafficLights.getStoplichten().get(group_key.toString());

            for (Trafficlight light : traffic_lights) {
                if (light.getLightState() == LightState.groen || light.getLightState() == LightState.oranje)
                    return true;
            }
        }

        return false;
    }

    public boolean meetsTransitionRequirements(String group_key, IntersectionData.Group group, SensorSpecial sensorSpecial) {
        // If a group doesn't have transition requirements then we can return true by default
        IntersectionData.TransitionRequirements transition_requirements = group.getTransitionRequirements();
        IntersectionData.TransitionRequirements transition_blockers = group.getTransitionBlockers();

        if (transition_requirements == null && transition_blockers == null) return true;

        // Check if transition requirements are met
        boolean requirements_met = true;
        if (transition_requirements != null) {
            var green_transition_requirements = transition_requirements.getGreen();

            for (IntersectionData.TransitionRequirement tr : green_transition_requirements) {
                switch (tr.getSensor().toLowerCase()) {
                    case "sensor":
                        if (getSpecialSensorValue(tr.getSensor(), sensorSpecial) != tr.getSensorState())
                            requirements_met = false;
                        break;
                    case "other_traffic_light":
                        if (!trafficLights.getStoplichten().get(group_key).get(0).getLightState().toString().equals(tr.getTrafficLightState()))
                            requirements_met = false;
                        break;
                }

                if (!requirements_met) break;
            }
        }

        // Check if transition blocker requirements are met
        // Check if ALL blocker requirements have been met, then it is NOT possible
        boolean blocker_requirements_met = true;
        if (transition_blockers != null) {
            var green_transition_requirements = transition_blockers.getGreen();

            int matched = 0;
            int total = green_transition_requirements.size();

            for (IntersectionData.TransitionRequirement tr : green_transition_requirements) {
                switch (tr.getType().toLowerCase()) {
                    case "sensor":
                        if (tr.getSensor().equals("brug_file_ver_A"))
                            break;

                        if (tr.getSensor().equals("brug_file_ver_B"))
                            break;

                        if (getSpecialSensorValue(tr.getSensor(), sensorSpecial) == tr.getSensorState())
                            matched++;
                        break;
                    case "other_traffic_light":
                        if (trafficLights.getStoplichten()
                                .get(group_key)
                                .get(0)
                                .getLightState()
                                .toString()
                                .equals(tr.getTrafficLightState()))
                            matched++;
                        break;
                }
            }

            if (matched == total)
                blocker_requirements_met = false;
        }

        // A transition is only allowed if:
        // - All required conditions are met
        // - Not all blockers are active
        return requirements_met && blocker_requirements_met;
    }

    public boolean getSpecialSensorValue(String sensorName, SensorSpecial sensorSpecial) {
        return switch (sensorName.toLowerCase()) {
            case "brug_wegdek" -> sensorSpecial.isBridge_road_value();
            case "brug_water" -> sensorSpecial.isBridge_water_value();
            case "brug_file" -> sensorSpecial.isBridge_traffic_value();
            default -> throw new IllegalArgumentException("Sensor naam is invalide: [" + sensorName + "] Controleer de naam met de spec.");
        };
    }

    private void sendTrafficLightsToPublisher() throws JsonProcessingException, JSONException {
        JSONObject json = new JSONObject();

        Enumeration<String> Keys = trafficLights.getStoplichten().keys();
        while (Keys.hasMoreElements()) {
            String key = Keys.nextElement();
            List<Trafficlight> groupTrafficLights = trafficLights.getStoplichten().get(key);

            for (Trafficlight light : groupTrafficLights) {
                json.put(light.getLightId(), light.getLightState().toString());
            }
        }

        System.out.println("Sent: [" + time.getMs() + "] [" + "stoplichten" + "] " + json);

        zmqPublisher.sendMessage("stoplichten", json.toString());
    }
}