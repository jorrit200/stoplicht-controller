package com.stoplicht_controller.stoplicht_controller.Configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@Configuration
public class ZmqPublisher {
    private final ZContext context;
    private final ZMQ.Socket publisherSocket;
    private final String adress = "tcp://127.0.0.1:5556";

    public ZmqPublisher() {
        this.context = new ZContext();
        this.publisherSocket = context.createSocket(SocketType.PUB);
        this.publisherSocket.bind(adress);

    }

    public void sendMessage(String topic, String message) {
        this.publisherSocket.sendMore(topic.getBytes());
        this.publisherSocket.send(message.getBytes(ZMQ.CHARSET), 0);
        //System.out.println("Sent: " + message);
    }

    public void close() {
        this.publisherSocket.close();
    }


}
