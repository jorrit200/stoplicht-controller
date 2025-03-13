package com.stoplicht_controller.stoplicht_controller.Services;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.*;

class JsonReaderTest {
    @Test
    void readJson() {

         System.out.println(JsonReader.getTrafficLightConfigFromSpec().getGroups().keySet().stream().map(x -> x.getClass().toString()).collect(Collectors.joining(", ")));

    }
}