package com.stoplicht_controller.stoplicht_controller.Configurations;

import org.springframework.beans.factory.annotation.Configurable;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@Configurable
public class ZmqPublisher {
    private final ZContext context;
    private final ZMQ.Socket publisherSocket;

    public ZmqPublisher() {
        this.context = new ZContext();
        this.publisherSocket = context.createSocket(SocketType.PUB);
        this.publisherSocket.bind("tcp://*:5556");
    }

    public void sendMessage(String topic, String message) {
        this.publisherSocket.sendMore(topic.getBytes());
        this.publisherSocket.send(message.getBytes(ZMQ.CHARSET), 0);
        System.out.println("Sent: " + message);
    }

    public void close() {
        this.publisherSocket.close();
    }
}
