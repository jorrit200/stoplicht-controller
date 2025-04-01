package com.stoplicht_controller.stoplicht_controller.Models;

import com.stoplicht_controller.stoplicht_controller.Enums.TrafficlightState;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Dictionary;
import java.util.Hashtable;

@Getter
@Setter
@Component
public class Stoplicht {
    private Dictionary<String, TrafficlightState> stoplichten = new Hashtable<String, TrafficlightState>();

    public Stoplicht() {}
}
