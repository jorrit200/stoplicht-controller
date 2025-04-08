package com.stoplicht_controller.stoplicht_controller.Models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Dictionary;
import java.util.Hashtable;

@Getter
@Setter
@Component
public class TrafficlightData {
    private Dictionary<String, Trafficlight> stoplichten = new Hashtable<String, Trafficlight>();

    public TrafficlightData() {}
}
