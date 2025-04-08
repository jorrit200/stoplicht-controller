package com.stoplicht_controller.stoplicht_controller.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Configurations.*;
import com.stoplicht_controller.stoplicht_controller.Dtos.SensorLane;
import com.stoplicht_controller.stoplicht_controller.Dtos.SensorSpecial;
import com.stoplicht_controller.stoplicht_controller.Dtos.Time;
import com.stoplicht_controller.stoplicht_controller.Dtos.PriorityVehicleQueue;
import com.stoplicht_controller.stoplicht_controller.Enums.LightState;
import com.stoplicht_controller.stoplicht_controller.Models.*;
import com.stoplicht_controller.stoplicht_controller.messaging.JsonMessageReceiver;
import com.stoplicht_controller.stoplicht_controller.Util.JsonReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
public class TrafficlightController {
    @Autowired
    private JsonMessageReceiver jsonMessageReceiver;

    @Autowired
    private ZmqPublisher zmqPublisher;
    @Autowired
    private TrafficlightData trafficLights;

    ObjectMapper objectMapper = new ObjectMapper();

    IntersectionData intersectionData = JsonReader.getTrafficLightConfigFromSpec();

    public TrafficlightController() {}


    public void start() {
        /// Puntensysteem, intersectie punten aftrek?
        /// iets met een cyclus
        ///  iets met tijd
        while(true){
            try {
                System.out.println("Loop started in controller");
                SensorLane sensorLane = jsonMessageReceiver.receiveMessage("sensoren_rijbaan", SensorLane.class);

                Time time = jsonMessageReceiver.receiveMessage("tijd", Time.class);
                PriorityVehicleQueue priorityVehicleQueue = jsonMessageReceiver.receiveMessage("voorrangsvoertuig", PriorityVehicleQueue.class);
                SensorSpecial sensorSpecial = jsonMessageReceiver.receiveMessage("sensoren_speciaal", SensorSpecial.class);

                if(!priorityVehicleQueue.getQueue().isEmpty())
                {
                    processPriorityVehicle(priorityVehicleQueue);
                }
                //Cycles
                startCycle(time, priorityVehicleQueue, sensorLane, sensorSpecial);

                String trafficLightsJson = objectMapper.writeValueAsString(trafficLights);
                System.out.println("about to send message");

                zmqPublisher.sendMessage("stoplichten", trafficLightsJson );

            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    public void startCycle(Time time, PriorityVehicleQueue priorityVehicleQueue, SensorLane sensorLane, SensorSpecial sensorSpecial) {

        // Loop over alle verkeerslichtgroepen
        for(Integer groupkey : intersectionData.getGroups().keySet()){
            var group = intersectionData.getGroups().get(groupkey);

            // 1. Conflictdetectie met Streams: Controleer of een conflicterende groep al op groen staat
            var conflict = group.getIntersectsWith().stream()
                    .map(Object::toString)
                    //haal uit de trafficlights conflicterende groepen en check of ze groen zijn, trafficLights wordt constant geupdate in een loop dus dit gebeurt uiteindelijk
                    .anyMatch(conflictGroup-> trafficLights.getStoplichten().get(conflictGroup).getLightState() == LightState.groen);
                            //getStoplichten().get(conflictGroup) == LightState.groen);

            // 2. Controleer of de transitievereisten zijn voldaan
            boolean requirementsMet = checkTransitionRequirements(group, sensorSpecial, sensorLane, priorityVehicleQueue);

            // 3. Als er geen conflicten zijn en de transitievereisten zijn voldaan, zet het verkeerslicht op groen
            if (!conflict && requirementsMet) {
                trafficLights.getStoplichten().put(groupkey.toString(), new Trafficlight(LightState.groen));
            } else {
                trafficLights.getStoplichten().put(groupkey.toString(), new Trafficlight(LightState.rood));
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
            case "brug_wegdek":
                return sensorSpecial.isBrug_wegdek();
            case "brug_water":
                return sensorSpecial.isBrug_water();
            case "brug_file":
                return sensorSpecial.isBrug_file();
            default:
                SensorLane.SensorStatus sensorStatus = sensorLane.getSensors().get(sensorName);
                if (sensorStatus != null) {
                    // Hier kiezen we ervoor om de 'voor'-waarde terug te geven; pas dit aan indien nodig.
                    return sensorStatus.isVoor();
                } else {
                    throw new IllegalArgumentException("Sensor niet gevonden: " + sensorName);
                }
        }
    }

    public void processPriorityVehicle(PriorityVehicleQueue priorityVehicleQueue) throws JsonProcessingException {

        for (PriorityVehicleQueue.Voorrangsvoertuig voertuig : priorityVehicleQueue.getQueue()){
            String laneId = voertuig.getBaan();
            //get groupKey
            String groupKey = laneId;
            if(groupKey != null){
                var group = intersectionData.getGroups().get(groupKey);
                var conflict = group.getIntersectsWith().stream()
                        .anyMatch(conflictGroup -> trafficLights.getStoplichten().get(conflictGroup).getLightState()  == LightState.groen);

                if(!conflict){
                    trafficLights.getStoplichten().put(groupKey, new Trafficlight(LightState.groen));
                }
            }
        }

        zmqPublisher.sendMessage("stoplichten", trafficLightsJson(trafficLights.getStoplichten()));
        start();
    }

    //todo check if its right
    public String trafficLightsJson(Dictionary<String, Trafficlight> trafficLights) throws JsonProcessingException {

        Map<String, Trafficlight> trafficLightsMap = new HashMap<>();

        Enumeration<String> keys = trafficLights.keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement(); //key
            trafficLightsMap.put(key, trafficLights.get(key));
        }

        return objectMapper.writeValueAsString(trafficLightsMap);
    }
}
