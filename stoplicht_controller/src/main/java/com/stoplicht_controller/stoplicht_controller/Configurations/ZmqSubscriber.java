package com.stoplicht_controller.stoplicht_controller.Configurations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Configuration
public class ZmqSubscriber {
    private final ZContext context;
    private final ZMQ.Socket subscriber;
    private final String address;
    private ObjectMapper om = new ObjectMapper();

    public ZmqSubscriber() throws IOException {
        context = new ZContext();
        subscriber = context.createSocket(SocketType.SUB);

        // Read the JSON file
        String json = Files.readString(Path.of("src/main/resources/configuration-format.json"));

        // Create ObjectMapper to map the JSON
        ObjectMapper mapper = new ObjectMapper();

        // Map the JSON
        Map<String, Map<String, String>> data = mapper.readValue(json, new TypeReference<>() {});
        Map<String, String> controller = data.get("simulator");

        // Define the address to connect to
        this.address = "tcp://" + controller.get("host") + ":" + controller.get("port");

        subscriber.connect(this.address);
    }

    public String receiveMessage(String topic) {
        subscriber.subscribe(topic.getBytes(ZMQ.CHARSET));
        String contents = subscriber.recvStr(100).trim();
        System.out.println(contents);
        return contents;
    }
}
