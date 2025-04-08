package com.stoplicht_controller.stoplicht_controller.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PriorityVehicleQueue {
    @JsonProperty("queue")
    private List<Voorrangsvoertuig> queue;

    @Getter
    @Setter
    public class Voorrangsvoertuig {
        private String baan;
        private int simulatie_tijd_ms;
        private int positie;
    }
}
