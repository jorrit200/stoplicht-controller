package com.stoplicht_controller.stoplicht_controller.Controllers;

import com.stoplicht_controller.stoplicht_controller.Configurations.*;
import com.stoplicht_controller.stoplicht_controller.Models.SensorenRijbaan;
import com.stoplicht_controller.stoplicht_controller.Models.SensorenSpeciaal;
import com.stoplicht_controller.stoplicht_controller.Models.Tijd;
import com.stoplicht_controller.stoplicht_controller.Models.VoorrangsvoertuigRij;
import com.stoplicht_controller.stoplicht_controller.Services.JsonMessageReceiver;
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
