package FowlFlightForensics.util.serdes;

import FowlFlightForensics.domain.IncidentSummary;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class JsonValueSerde extends Serdes.WrapperSerde<IncidentSummary> {
    public JsonValueSerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(IncidentSummary.class));
    }
}
