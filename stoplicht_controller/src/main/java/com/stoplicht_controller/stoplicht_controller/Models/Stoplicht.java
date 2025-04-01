package com.stoplicht_controller.stoplicht_controller.Models;

import com.stoplicht_controller.stoplicht_controller.Enums.LightState;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Dictionary;
import java.util.Hashtable;

@Getter
@Setter
@Component
public class Stoplicht {
    private Dictionary<String, TrafficLight> stoplichten = new Hashtable<String, TrafficLight>();
//make new object trafficlight met trafficlight state en prioritering


    public Stoplicht() {}
}
