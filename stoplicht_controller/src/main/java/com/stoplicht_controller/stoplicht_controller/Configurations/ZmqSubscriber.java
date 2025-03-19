package com.stoplicht_controller.stoplicht_controller.Configurations;

import org.springframework.context.annotation.Configuration;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@Configuration
public class ZmqSubscriber {
    private final ZContext context;
    private final ZMQ.Socket subscriber;

    public ZmqSubscriber() {
        context = new ZContext();
        subscriber = context.createSocket(SocketType.SUB);
        subscriber.connect("tcp://localhost:5558");
    }

    public String receiveMessage(String topic) {
        subscriber.subscribe(topic.getBytes(ZMQ.CHARSET));
        String contents = subscriber.recvStr(100).trim();
        System.out.println(contents);
        return contents;
    }
}
