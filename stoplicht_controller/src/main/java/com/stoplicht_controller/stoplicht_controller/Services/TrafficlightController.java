package com.stoplicht_controller.stoplicht_controller.Services;

import Config.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqPublisher;
import com.stoplicht_controller.stoplicht_controller.Configurations.ZmqSubscriber;
import com.stoplicht_controller.stoplicht_controller.Util.JsonReader;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrafficlightController {
    @Autowired
    private JsonMessageReceiver jsonMessageReceiver;

    @Autowired
    private ZmqPublisher zmqPublisher;

    public TrafficlightController() {}



    public void start() {
        /// Puntensysteem, intersectie punten aftrek?
        /// iets met een cyclus
        while(true){
            try {

                Tijd tijd = jsonMessageReceiver.receiveMessage("tijd", Tijd.class);
                VoorrangsvoertuigRij voorrangsvoertuigRij = jsonMessageReceiver.receiveMessage("voorrangsvoertuig", VoorrangsvoertuigRij.class);
                SensorenRijbaan sensorenRijbaan = jsonMessageReceiver.receiveMessage("sensoren_rijbaan", SensorenRijbaan.class);
                SensorenSpeciaal sensorenSpeciaal = jsonMessageReceiver.receiveMessage("sensorenSpeciaal", SensorenSpeciaal.class);





            }catch (Exception e){
                e.printStackTrace();
            }
            //zmqPublisher.sendMessage("stoplichten", "tobeimplementedjsonformat" );
        }
    }


}
