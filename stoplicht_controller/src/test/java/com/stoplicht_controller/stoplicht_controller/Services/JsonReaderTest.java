package com.stoplicht_controller.stoplicht_controller.Services;

import com.stoplicht_controller.stoplicht_controller.Util.JsonReader;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

class JsonReaderTest {
    @Test
    void readJson() {

         System.out.println(JsonReader.getTrafficLightConfigFromSpec().getGroups().keySet().stream().map(x -> x.getClass().toString()).collect(Collectors.joining(", ")));

    }
}