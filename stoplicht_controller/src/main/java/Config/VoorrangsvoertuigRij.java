package Config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class VoorrangsvoertuigRij {
    @JsonProperty("queue")
    private List<Voorrangsvoertuig> queue;

    @Getter
    @Setter
    public class Voorrangsvoertuig {
        private String baan;
        private int simulatie_tijd_ms;
    }
}
