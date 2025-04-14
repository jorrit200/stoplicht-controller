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
    private final String adress = "tcp://127.0.0.1:5558";
    private ObjectMapper objectMapper = new ObjectMapper();

    public TestPublisher() {
        this.context = new ZContext();
        this.publisherSocket = context.createSocket(SocketType.PUB);
        this.publisherSocket.bind(adress);

    }

    public void sendMessage(String topic, String message) throws Exception {
        this.publisherSocket.sendMore(topic);
        this.publisherSocket.send(message);
        System.out.println("Sent: [" + topic + "] " + message);
        Thread.sleep(1000);
    }

    public void startLoop() {
        System.out.println("loop started in publisher");
        while (true) {
            // Initialize SensorenRijbaan
            SensorLane sensorRijbaan = new SensorLane();
            Map<String, SensorLane.SensorStatus> sensors = new HashMap<>();
            SensorLane.SensorStatus sensorStatus = new SensorLane.SensorStatus();
            sensorStatus.setFront(true);
            sensorStatus.setBack(false);
            sensors.put("1.1", sensorStatus);
            sensorRijbaan.setSensors(sensors);

            // Initialize SensorenSpeciaal
            SensorSpecial sensorSpeciaal = new SensorSpecial();
            sensorSpeciaal.setBridge_road(true);
            sensorSpeciaal.setBridge_water(false);
            sensorSpeciaal.setBridge_traffic(true);

            // Initialize Tijd
            Time time = new Time();
            time.setMs(500);

            // Initialize VoorrangsvoertuigRij
            PriorityVehicleQueue priorityVehicleQueue = new PriorityVehicleQueue();
            List<PriorityVehicleQueue.PriorityVehicle> queue = new ArrayList<>();
            PriorityVehicleQueue.PriorityVehicle voertuig = new PriorityVehicleQueue.PriorityVehicle();
            voertuig.setLane("8.1");
            voertuig.setSimulation_time_ms(500);
            voertuig.setPriority(1);
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

}