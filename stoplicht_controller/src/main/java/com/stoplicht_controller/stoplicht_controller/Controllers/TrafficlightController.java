package com.stoplicht_controller.stoplicht_controller.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqPublisher;
import com.stoplicht_controller.stoplicht_controller.Dtos.*;
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

    private SensorLane sensorLane = new SensorLane();
    private Time time = new Time();
    private PriorityVehicleQueue priorityVehicleQueue = new PriorityVehicleQueue();
    private SensorSpecial sensorSpecial = new SensorSpecial();
    private SensorBridge sensorBridge = new SensorBridge();

    private boolean hasSentOrange = false;
    private boolean hasSentRed = false;
    private boolean ActiveAmbulance = false;

    private Map<String, Class<?>> topicClassMap = Map.of(
            "sensoren_rijbaan", SensorLane.class,
            "sensoren_speciaal", SensorSpecial.class,
            "tijd", Time.class,
            "voorrangsvoertuig", PriorityVehicleQueue.class,
            "sensoren_bruggen", SensorBridge.class
    );

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
                } else if (castedMessage instanceof  SensorBridge) {
                    this.sensorBridge = (SensorBridge) castedMessage;
                } else {
                    System.out.println("Unknown message type received: " + castedMessage.getClass());
                }

                if (!Objects.equals(topic, "tijd"))
                    System.out.println("Received: [" + topic + "] ");

                trafficCycle(sensorLane, time, priorityVehicleQueue, sensorSpecial);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void trafficCycle(SensorLane sensorLane, Time time,
                             PriorityVehicleQueue priorityVehicleQueue, SensorSpecial sensorSpecial)
            throws Exception {
        // If there is a priority queue
        if (priorityVehicleQueue.getQueue() != null) {
            // Ensure queue is sorted by lowest simulation time first
            priorityVehicleQueue.sortQueueBySimulationTime();

            // For each priority vehicle in the queue
            for (PriorityVehicleQueue.PriorityVehicle vehicle : priorityVehicleQueue.getQueue()) {
                // Switch functionality depending on priority
                switch (vehicle.getPriority()) {
                    case 1: { // Ambulance
                        if (!ActiveAmbulance) {
                            // Set hadAmbulance to true, so we don't crash ambulances
                            ActiveAmbulance = true;

                            // Add weight to ambulance lane, to ensure it will always go green on next cycle
                            AddWeightToLane(vehicle.getLane(), 5);
                        }
                    }
                    case 2: { // Bus
                        AddWeightToLane(vehicle.getLane(), 2);
                    }
                }
            }
        }

        LightStateSwitchCheck(sensorLane, time, sensorSpecial);
    }

    private void LightStateSwitchCheck(SensorLane sensorLane, Time time, SensorSpecial sensorSpecial) throws Exception {
        // Check if time != null
        if (time != null) {
            // Transition time
            if ((time.getMs() - simulation_time_last_updated_ms >= 13000)) {
                GenerateGreenLightCombination(sensorLane, sensorSpecial, trafficLights);

                simulation_time_last_updated_ms = time.getMs();
                hasSentRed = false;

                sendTrafficLightsToPublisher(trafficLights);
            }

            // Switch orange to red
            if ((time.getMs() - simulation_time_last_updated_ms >= 10500 &&
                    time.getMs() - simulation_time_last_updated_ms < 13000 && !hasSentRed)){

                ChangeTrafficLights(trafficLights, LightState.rood);
                hasSentOrange = false;
                hasSentRed = true;

                sendTrafficLightsToPublisher(trafficLights);
            }

            // Switch green to orange
            if ((time.getMs() - simulation_time_last_updated_ms >= 7000 &&
                    time.getMs() - simulation_time_last_updated_ms < 10500 && !hasSentOrange)) {
                // Set lights of current to orange
                ChangeTrafficLights(trafficLights, LightState.oranje);
                hasSentOrange = true;
                ActiveAmbulance = false;

                sendTrafficLightsToPublisher(trafficLights);
            }
        }
    }

    public void AddWeightToLane(String lane, int weight) {
        String laneKey = lane.split("\\.")[0];

        List<Trafficlight> laneGroup = trafficLights.getStoplichten().get(laneKey);

        laneGroup.get(0).addWeight(weight);
    }

    public void ChangeTrafficLights(TrafficlightData lights, LightState change_light_to) throws Exception {
        // Enumeration for traffic lights
        Enumeration<String> keys = lights.getStoplichten().keys();

        while (keys.hasMoreElements()) {
            String groupKey = keys.nextElement();
            List<Trafficlight> trafficLightsFromGroup = lights.getStoplichten().get(groupKey);

            for (Trafficlight light : trafficLightsFromGroup) {
                switch (change_light_to) {
                    // If light is red, change to orange
                    case oranje:
                        if (light.getLightState() == LightState.groen && meetsTransitionRequirements(
                                groupKey,
                                intersectionData.getGroups().get(Integer.parseInt(groupKey)),
                                sensorSpecial,
                                "red"
                                )) {
                            light.setLightState(LightState.oranje);
                        }
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

    public void GenerateGreenLightCombination(SensorLane sensorLane, SensorSpecial sensorSpecial, TrafficlightData trafficlightData) throws Exception {
        // Check whether SensorLane had sensor changes
        for (String key : sensorLane.sensors.keySet()) {
            var old_state = sensorLane.sensors.get(key);
            var new_state = this.sensorLane.sensors.get(key);

            // If there is a boat, add lots of weight to give it a high prio
            if (key.equals("71.1") || key.equals("72.1")) {
                // If front sensor is true
                if (!old_state.isFront() && new_state.isFront())
                    AddWeightToLane(key, 25);

                // back sensor true
                if (!old_state.isBack() && new_state.isBack())
                    AddWeightToLane(key, 1);
            } else {
                // if front sensor became true, add 2 weight
                if (!old_state.isFront() && new_state.isFront()) {
                    AddWeightToLane(key, 2);
                }

                // If back sensor became true, add 1 weight
                if (!old_state.isBack() && new_state.isBack())
                    AddWeightToLane(key, 1);
            }

            // If back sensor stays true, add 2 weight
            if (!old_state.isBack() && new_state.isBack()) {
                AddWeightToLane(key, 2);
            }
        }

        // Update lane state tracked by controller
        this.sensorLane = sensorLane;

        // Create a list that orders groups by the sum of the lane weight
        Map<String, Integer> group_weight = new HashMap<>();

        Enumeration<String> keys = trafficlightData.getStoplichten().keys();

        while (keys.hasMoreElements()) {
            String group_key = keys.nextElement();
            List<Trafficlight> lights = trafficlightData.getStoplichten().get(group_key);

            int weight_sum = 0;
            for (Trafficlight light : lights) {
                weight_sum += light.getWeight();
            }

            group_weight.put(group_key, weight_sum);
        }

        // Sort the group_weight descending
        List<Map.Entry<String, Integer>> sorted_group_weight = new ArrayList<>(group_weight.entrySet());
        sorted_group_weight.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

        List<Trafficlight> heaviest_group = trafficlightData.getStoplichten()
                .get(sorted_group_weight.get(0).getKey());

        // Set heaviest group lights to green
        for (Trafficlight light : heaviest_group) {
            light.setLightState(LightState.groen);
        }

        // Set other possible groups to green
        for (Map.Entry<String,Integer> group_with_weight : sorted_group_weight) {
            if (Objects.equals(group_with_weight.getKey(), sorted_group_weight.get(0).getKey())) continue;

            var group = intersectionData.getGroups().get(Integer.parseInt(group_with_weight.getKey()));

            boolean group_has_conflict = hasGroupConflicts(group);
            boolean transition_requirements_met = meetsTransitionRequirements(group_with_weight.getKey(), group, sensorSpecial, "green");

            if (!group_has_conflict && transition_requirements_met) {
                List<Trafficlight> lights_in_group = trafficlightData.getStoplichten().get(group_with_weight.getKey());

                // Only set bridge green if there are boats
                if (group_with_weight.getKey().equals("71") || group_with_weight.getKey().equals("72") || group_with_weight.getKey().equals("81")) {
                    var sensor = sensorLane.getSensors().get(group_with_weight.getKey() + ".1");
                    if (sensor != null && sensor.isFront()) {
                        for (Trafficlight tl : lights_in_group) {
                            tl.setLightState(LightState.groen);
                            tl.setWeight(0);
                        }
                    }
                } else {
                    for (Trafficlight tl : lights_in_group) {
                        tl.setLightState(LightState.groen);
                        tl.setWeight(0);
                    }
                }
            }
        }

        // Give remaining red lights extra weight
        keys = trafficlightData.getStoplichten().keys();
        while (keys.hasMoreElements()) {
            String group_key = keys.nextElement();
            List<Trafficlight> lights = trafficlightData.getStoplichten().get(group_key);

            if (group_key.equals("71") || group_key.equals("72"))
                if (lights.get(0).getLightState() == LightState.groen){
                    trafficlightData.getStoplichten().get("81").get(0).setLightState(LightState.groen);
                }

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

    private List<IntersectionData.TransitionRequirement> GetTransitionRequirements (String color, IntersectionData.TransitionRequirements reqs) throws Exception {
        return switch (color) {
            case "green" -> reqs.getGreen();
            case "red" -> reqs.getRed();
            case "orange" -> reqs.getOrange();
            default -> throw new Exception("Invalid color");
        };
    }

    public boolean meetsTransitionRequirements(String group_key, IntersectionData.Group group, SensorSpecial sensorSpecial, String color) throws Exception {
        // If a group doesn't have transition requirements then we can return true by default
        IntersectionData.TransitionRequirements transition_requirements = group.getTransitionRequirements();
        IntersectionData.TransitionRequirements transition_blockers = group.getTransitionBlockers();

        if (transition_requirements == null && transition_blockers == null) return true;

        // Check if transition requirements are met
        boolean requirements_met = true;
        if (transition_requirements != null) {
            var transitionRequirements = GetTransitionRequirements(color, transition_requirements);

            for (IntersectionData.TransitionRequirement tr : transitionRequirements) {
                switch (tr.getType().toLowerCase()) {
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
            var color_transition_blockers = GetTransitionRequirements(color, transition_blockers);

            if (color_transition_blockers != null) {
                int matched = 0;
                int total = color_transition_blockers.size();

                for (IntersectionData.TransitionRequirement tr : color_transition_blockers) {
                    switch (tr.getType().toLowerCase()) {
                        case "sensor":
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
            default -> {
                System.out.println("ERROR: " + "Sensor naam is invalide: [" + sensorName + "] Controleer de naam met de spec.");
                yield true;
            }
        };
    }

    private void sendTrafficLightsToPublisher(TrafficlightData lights) throws JSONException {
        JSONObject json = new JSONObject();

        Enumeration<String> Keys = lights.getStoplichten().keys();
        while (Keys.hasMoreElements()) {
            String key = Keys.nextElement();
            List<Trafficlight> groupTrafficLights = lights.getStoplichten().get(key);

            for (Trafficlight light : groupTrafficLights) {
                json.put(light.getLightId(), light.getLightState().toString());
            }
        }

        System.out.println("Sent: [" + time.getMs() + "] [stoplichten]"); //+ json);

        zmqPublisher.sendMessage("stoplichten", json.toString());
    }
}