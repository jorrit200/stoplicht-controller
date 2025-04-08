package com.stoplicht_controller.stoplicht_controller.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Configurations.*;
import com.stoplicht_controller.stoplicht_controller.Dtos.SensorenRijbaan;
import com.stoplicht_controller.stoplicht_controller.Dtos.SensorenSpeciaal;
import com.stoplicht_controller.stoplicht_controller.Dtos.Tijd;
import com.stoplicht_controller.stoplicht_controller.Dtos.VoorrangsvoertuigRij;
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
    private Stoplicht trafficLights;

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
                Tijd tijd = jsonMessageReceiver.receiveMessage("tijd", Tijd.class);
                VoorrangsvoertuigRij voorrangsvoertuigRij = jsonMessageReceiver.receiveMessage("voorrangsvoertuig", VoorrangsvoertuigRij.class);
                SensorenRijbaan sensorenRijbaan = jsonMessageReceiver.receiveMessage("sensoren_rijbaan", SensorenRijbaan.class);
                SensorenSpeciaal sensorenSpeciaal = jsonMessageReceiver.receiveMessage("sensoren_speciaal", SensorenSpeciaal.class);

                if(!voorrangsvoertuigRij.getQueue().isEmpty())
                {
                    processPriorityVehicle(voorrangsvoertuigRij);
                }
                //Cycles
                startCycle(tijd, voorrangsvoertuigRij,sensorenRijbaan,sensorenSpeciaal);

                String trafficLightsJson = objectMapper.writeValueAsString(trafficLights);
                System.out.println("about to send message");

                zmqPublisher.sendMessage("stoplichten", trafficLightsJson );

            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    public void startCycle(Tijd tijd, VoorrangsvoertuigRij voorrangsvoertuigRij, SensorenRijbaan sensorenRijbaan, SensorenSpeciaal sensorenSpeciaal) {

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
            boolean requirementsMet = checkTransitionRequirements(group, sensorenSpeciaal, sensorenRijbaan, voorrangsvoertuigRij);

            // 3. Als er geen conflicten zijn en de transitievereisten zijn voldaan, zet het verkeerslicht op groen
            if (!conflict && requirementsMet) {
                trafficLights.getStoplichten().put(groupkey.toString(), new TrafficLight(LightState.groen));
            } else {
                trafficLights.getStoplichten().put(groupkey.toString(), new TrafficLight(LightState.rood));
            }
        }
    }

    private boolean checkTransitionRequirements(IntersectionData.Group group, SensorenSpeciaal sensorenSpeciaal, SensorenRijbaan sensorenRijbaan, VoorrangsvoertuigRij voorrangsvoertuigRij) {
        // Als er geen transitievereisten zijn, is de overgang toegestaan
        if (group.getTransitionRequirements() == null)
            return true;

        // Controleer alle vereisten met Streams
        return group.getTransitionRequirements().getGreen().stream().allMatch(req -> {
            if ("sensor".equals(req.getType())) {
                boolean sensorValue = getSensorValue(req.getSensor(), sensorenSpeciaal, sensorenRijbaan);
                return sensorValue == req.getSensorState();
            }
            // Voeg hier extra voorwaarden toe indien nodig
            return true;
        });
    }

    public boolean getSensorValue(String sensorName, SensorenSpeciaal sensorenSpeciaal, SensorenRijbaan sensorenRijbaan) {
        switch (sensorName) {
            case "brug_wegdek":
                return sensorenSpeciaal.isBrug_wegdek();
            case "brug_water":
                return sensorenSpeciaal.isBrug_water();
            case "brug_file":
                return sensorenSpeciaal.isBrug_file();
            default:
                SensorenRijbaan.SensorStatus sensorStatus = sensorenRijbaan.getSensors().get(sensorName);
                if (sensorStatus != null) {
                    // Hier kiezen we ervoor om de 'voor'-waarde terug te geven; pas dit aan indien nodig.
                    return sensorStatus.isVoor();
                } else {
                    throw new IllegalArgumentException("Sensor niet gevonden: " + sensorName);
                }
        }
    }

    public void processPriorityVehicle(VoorrangsvoertuigRij voorrangsvoertuigRij) throws JsonProcessingException {

        for (VoorrangsvoertuigRij.Voorrangsvoertuig voertuig : voorrangsvoertuigRij.getQueue()){
            String laneId = voertuig.getBaan();
            //get groupKey
            String groupKey = laneId;
            if(groupKey != null){
                var group = intersectionData.getGroups().get(groupKey);
                var conflict = group.getIntersectsWith().stream()
                        .anyMatch(conflictGroup -> trafficLights.getStoplichten().get(conflictGroup).getLightState()  == LightState.groen);

                if(!conflict){
                    trafficLights.getStoplichten().put(groupKey, new TrafficLight(LightState.groen));
                }
            }
        }

        zmqPublisher.sendMessage("stoplichten", trafficLightsJson(trafficLights.getStoplichten()));
        start();
    }

    //todo check if its right
    public String trafficLightsJson(Dictionary<String, TrafficLight> trafficLights) throws JsonProcessingException {

        Map<String, TrafficLight> trafficLightsMap = new HashMap<>();

        Enumeration<String> keys = trafficLights.keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement(); //key
            trafficLightsMap.put(key, trafficLights.get(key));
        }

        return objectMapper.writeValueAsString(trafficLightsMap);
    }
}
