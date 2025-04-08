package com.stoplicht_controller.stoplicht_controller.Configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplicht_controller.stoplicht_controller.Dtos.SensorLane;
import com.stoplicht_controller.stoplicht_controller.Dtos.SensorSpecial;
import com.stoplicht_controller.stoplicht_controller.Dtos.Time;
import com.stoplicht_controller.stoplicht_controller.Dtos.PriorityVehicleQueue;
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
    private final String adress = "tcp://10.121.17.123:5558";
    private ObjectMapper objectMapper = new ObjectMapper();

    public TestPublisher() {
        this.context = new ZContext();
        this.publisherSocket = context.createSocket(SocketType.PUB);
        this.publisherSocket.bind(adress);

    }

    public void sendMessage(String topic, String message) {
        this.publisherSocket.sendMore(topic.getBytes());
        this.publisherSocket.send(message.getBytes(ZMQ.CHARSET), 0);
        //System.out.println("Sent: " + message);
    }

    public void startLoop() {
        // Initialize SensorenRijbaan
        SensorLane sensorRijbaan = new SensorLane();
        Map<String, SensorLane.SensorStatus> sensors = new HashMap<>();
        SensorLane.SensorStatus sensorStatus = sensorRijbaan.new SensorStatus();
        sensorStatus.setVoor(true);
        sensorStatus.setAchter(false);
        sensors.put("1.1", sensorStatus);
        sensorRijbaan.setSensors(sensors);

        // Initialize SensorenSpeciaal
        SensorSpecial sensorSpeciaal = new SensorSpecial();
        sensorSpeciaal.setBrug_wegdek(true);
        sensorSpeciaal.setBrug_water(false);
        sensorSpeciaal.setBrug_file(true);

        // Initialize Tijd
        Time time = new Time();
        time.setMs(500);

        // Initialize VoorrangsvoertuigRij
        PriorityVehicleQueue priorityVehicleQueue = new PriorityVehicleQueue();
        List<PriorityVehicleQueue.Voorrangsvoertuig> queue = new ArrayList<>();
        PriorityVehicleQueue.Voorrangsvoertuig voertuig = priorityVehicleQueue.new Voorrangsvoertuig();
        voertuig.setBaan("baan1");
        voertuig.setSimulatie_tijd_ms(500);
        voertuig.setPositie(1);
        queue.add(voertuig);
        priorityVehicleQueue.setQueue(queue);

        try {
            String tijdMessage = objectMapper.writeValueAsString(time);
            this.sendMessage("tijd", tijdMessage);


            // Send VoorrangsvoertuigRij
            String voorrangsvoertuigRijMessage = objectMapper.writeValueAsString(priorityVehicleQueue);
            this.sendMessage("voorrangsvoertuig", voorrangsvoertuigRijMessage);


            // Send SensorenRijbaan
            String sensorRijbaanMessage = objectMapper.writeValueAsString(sensorRijbaan);
            this.sendMessage("sensoren_rijbaan", sensorRijbaanMessage);

            // Send SensorenSpeciaal
            String sensorSpeciaalMessage = objectMapper.writeValueAsString(sensorSpeciaal);
            this.sendMessage("sensoren_speciaal", sensorSpeciaalMessage);



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
