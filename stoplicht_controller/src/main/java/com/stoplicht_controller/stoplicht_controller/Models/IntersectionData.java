package com.stoplicht_controller.stoplicht_controller.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntersectionData {
    private Map<Integer, Group> groups;
    private Map<String, Sensor> sensors;

    @Getter
    public static class Group {
        @JsonProperty("intersects_with")
        private List<Integer> intersectsWith;
        @JsonProperty("is_inverse_of")
        private Object isInverseOf; // Kan een boolean, nummer of string zijn
        @JsonProperty("extends_to")
        private Object extendsTo; // Kan een boolean of lijst zijn
        @JsonProperty("vehicle_type")
        private List<String> vehicleType;
        private Map<String, Lane> lanes;
        @JsonProperty("is_physical_barrier")
        private boolean isPhysicalBarrier;
        @JsonProperty("transition_requirements")
        private TransitionRequirements transitionRequirements;
        @JsonProperty("transition_blockers")
        private TransitionRequirements transitionBlockers;
    }

    @Getter
    public static class Lane {
        @JsonProperty("is_inverse_of")
        private Object isInverseOf; // Kan een nummer of string zijn
        @JsonProperty("extends_to")
        private Object extendsTo; // Kan een nummer of string zijn
    }

    @Getter
    public static class Sensor {
        private List<String> vehicles;
    }

    @Getter
    public static class TransitionRequirements {
        private List<TransitionRequirement> green;
        private List<TransitionRequirement> orange;
        private List<TransitionRequirement> red;
    }

    @Getter
    public static class TransitionRequirement {
        private String type;
        private String sensor;
        @JsonProperty("sensor_state")
        private Boolean sensorState;
        private Integer group;
        @JsonProperty("traffic_light_state")
        private String trafficLightState;
    }
}
