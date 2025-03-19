package Config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorenRijbaan {
    private Map<String, SensorStatus> sensors;

    @Getter
    public class SensorStatus {
        private boolean voor;
        private boolean achter;
    }
}
