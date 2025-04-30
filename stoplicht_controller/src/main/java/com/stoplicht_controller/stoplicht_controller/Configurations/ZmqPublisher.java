package com.stoplicht_controller.stoplicht_controller.Configurations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Configuration
public class ZmqPublisher {
    private final ZContext context;
    private final ZMQ.Socket publisherSocket;
    private final String address;

    public ZmqPublisher() throws IOException, URISyntaxException {
        context = new ZContext();

        // Read the JSON file
        String json = Files.readString(Path.of(getClass().getResource("/configuration-format.json").toURI()));

        // Create ObjectMapper to map the JSON
        ObjectMapper mapper = new ObjectMapper();

        // Map the JSON
        Map<String, Map<String, String>> data = mapper.readValue(json, new TypeReference<>() {});
        Map<String, String> controller = data.get("controller");

        // Define the address to connect to
        this.address = "tcp://" + controller.get("host") + ":" + controller.get("port");
        //System.out.println(address);

        this.publisherSocket = context.createSocket(SocketType.PUB);
        this.publisherSocket.bind(address);
    }

    public void sendMessage(String topic, String message) {
        this.publisherSocket.sendMore(topic);
        this.publisherSocket.send(message);
    }

    public void close() {
        this.publisherSocket.close();
    }


}
