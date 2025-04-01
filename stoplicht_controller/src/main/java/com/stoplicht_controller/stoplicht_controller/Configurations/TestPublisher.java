package com.stoplicht_controller.stoplicht_controller.Configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Dtos.SensorenRijbaan;
import com.stoplicht_controller.stoplicht_controller.Dtos.SensorenSpeciaal;
import com.stoplicht_controller.stoplicht_controller.Dtos.Tijd;
import com.stoplicht_controller.stoplicht_controller.Dtos.VoorrangsvoertuigRij;
import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TestPublisher {
    private final ZContext context;
    private final ZMQ.Socket publisherSocket;
    private final String adress = "tcp://*:5558";
    private ObjectMapper objectMapper = new ObjectMapper();

    public TestPublisher() {
        this.context = new ZContext();
        this.publisherSocket = context.createSocket(SocketType.PUB);
        this.publisherSocket.bind(adress);

    }

    public void sendMessage(String topic, String message) {
        this.publisherSocket.sendMore(topic.getBytes());
        this.publisherSocket.send(message.getBytes(ZMQ.CHARSET), 0);
        System.out.println("Sent: " + message);
    }

    public void startLoop() {
        // Initialize SensorenRijbaan
        SensorenRijbaan sensorRijbaan = new SensorenRijbaan();
        Map<String, SensorenRijbaan.SensorStatus> sensors = new HashMap<>();
        SensorenRijbaan.SensorStatus sensorStatus = sensorRijbaan.new SensorStatus();
        sensorStatus.setVoor(true);
        sensorStatus.setAchter(false);
        sensors.put("1.1", sensorStatus);
        sensorRijbaan.setSensors(sensors);

        // Initialize SensorenSpeciaal
        SensorenSpeciaal sensorSpeciaal = new SensorenSpeciaal();
        sensorSpeciaal.setBrug_wegdek(true);
        sensorSpeciaal.setBrug_water(false);
        sensorSpeciaal.setBrug_file(true);

        // Initialize Tijd
        Tijd tijd = new Tijd();
        tijd.setMs(500);

        // Initialize VoorrangsvoertuigRij
        VoorrangsvoertuigRij voorrangsvoertuigRij = new VoorrangsvoertuigRij();
        List<VoorrangsvoertuigRij.Voorrangsvoertuig> queue = new ArrayList<>();
        VoorrangsvoertuigRij.Voorrangsvoertuig voertuig = voorrangsvoertuigRij.new Voorrangsvoertuig();
        voertuig.setBaan("baan1");
        voertuig.setSimulatie_tijd_ms(500);
        voertuig.setPositie(1);
        queue.add(voertuig);
        voorrangsvoertuigRij.setQueue(queue);

        try {
            // Send SensorenRijbaan
            String sensorRijbaanMessage = objectMapper.writeValueAsString(sensorRijbaan);
            this.sendMessage("SensorenRijbaan", sensorRijbaanMessage);

            // Send SensorenSpeciaal
            String sensorSpeciaalMessage = objectMapper.writeValueAsString(sensorSpeciaal);
            this.sendMessage("SensorenSpeciaal", sensorSpeciaalMessage);

            // Send Tijd
            String tijdMessage = objectMapper.writeValueAsString(tijd);
            this.sendMessage("Tijd", tijdMessage);

            // Send VoorrangsvoertuigRij
            String voorrangsvoertuigRijMessage = objectMapper.writeValueAsString(voorrangsvoertuigRij);
            this.sendMessage("VoorrangsvoertuigRij", voorrangsvoertuigRijMessage);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
