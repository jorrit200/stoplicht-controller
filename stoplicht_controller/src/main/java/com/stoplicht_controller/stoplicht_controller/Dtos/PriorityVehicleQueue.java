package com.stoplicht_controller.stoplicht_controller.Dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PriorityVehicleQueue {
    private List<PriorityVehicle> queue;

    @Getter
    @Setter
    public class PriorityVehicle {
        private String lane;
        private int simulation_time_ms;
        private int position;
    }
}
