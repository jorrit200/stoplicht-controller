package com.stoplicht_controller.stoplicht_controller.Util;

import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@Component
public class ZmqSubscriber {
    private final ZContext context;
    private final ZMQ.Socket subscriber;

    public ZmqSubscriber() {
        context = new ZContext();
        subscriber = context.createSocket(SocketType.SUB);
        //todo determin what port to use
        subscriber.connect("tcp://localhost:5558");
    }

    public String receiveMessage(String topic) {
        subscriber.subscribe(topic.getBytes(ZMQ.CHARSET));
        String contents = subscriber.recvStr(0).trim();
        //todo test this
        System.out.println(contents);
        return contents;
    }
}
