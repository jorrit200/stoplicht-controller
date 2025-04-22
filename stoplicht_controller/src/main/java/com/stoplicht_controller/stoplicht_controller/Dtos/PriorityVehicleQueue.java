package com.stoplicht_controller.stoplicht_controller.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PriorityVehicleQueue {
    @JsonProperty("queue")
    private List<PriorityVehicle> queue = new ArrayList<PriorityVehicle>();

    @Getter
    @Setter
    public static class PriorityVehicle {
        @JsonProperty("baan")
        private String lane;
        @JsonProperty("simulatie_tijd_ms")
        private int simulation_time_ms;
        @JsonProperty("prioriteit")
        private int position;
    }
}
