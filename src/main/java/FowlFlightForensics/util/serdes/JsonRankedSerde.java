package FowlFlightForensics.util.serdes;

import FowlFlightForensics.domain.dto.IncidentRanked;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class JsonRankedSerde extends Serdes.WrapperSerde<IncidentRanked> {
    public JsonRankedSerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(IncidentRanked.class));
    }
}
