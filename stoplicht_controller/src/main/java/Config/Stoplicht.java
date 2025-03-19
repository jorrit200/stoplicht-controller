package Config;

import com.stoplicht_controller.stoplicht_controller.Enums.TrafficlightState;
import lombok.Getter;

import java.util.Map;

@Getter
public class Stoplicht {
    private Map<String, TrafficlightState> stoplichten;
}
