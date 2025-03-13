package com.stoplicht_controller.stoplicht_controller.services;

import org.springframework.stereotype.Service;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@Service
public class ZmqPublisher {
    @SuppressWarnings("FieldCanBeLocal")
    private final ZContext context;
    private final ZMQ.Socket publisherSocket;

    public ZmqPublisher() {
        // Create a ZMQ context
        this.context = new ZContext();
        // Create a PUB socket
        this.publisherSocket = context.createSocket(SocketType.PUB);
        // Bind the socket to an address
        this.publisherSocket.bind("tcp://*:5556");
    }

    public void sendMessage(String topic, String message) {
        String fullMessage = topic + " " + message;
        publisherSocket.send(fullMessage.getBytes(ZMQ.CHARSET), 0);
        System.out.println("Sent: " + fullMessage);
    }

    public void close() {
        publisherSocket.close();
    }
}
