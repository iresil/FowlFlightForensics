package FowlFlightForensics.util.serdes;

import FowlFlightForensics.domain.IncidentGrouped;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class JsonGroupedSerde extends Serdes.WrapperSerde<IncidentGrouped> {
    public JsonGroupedSerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(IncidentGrouped.class));
    }
}
