package com.stoplicht_controller.stoplicht_controller.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
public class PriorityVehicleQueue {
    private List<PriorityVehicle> queue = new ArrayList<>();

    @Getter
    @Setter
    public static class PriorityVehicle {
        @JsonProperty("baan")
        private String lane = "";
        @JsonProperty("simulatie_tijd_ms")
        private int simulation_time_ms = 0;
        @JsonProperty("prioriteit")
        private int priority = 0;
    }

    public void sortQueueBySimulationTime() {
        if (!queue.isEmpty()) {
            queue.sort(Comparator.comparingInt(PriorityVehicleQueue.PriorityVehicle::getSimulation_time_ms));
        }
    }
}
