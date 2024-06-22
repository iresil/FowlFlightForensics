package FowlFlightForensics.util.serdes;

import FowlFlightForensics.domain.dto.IncidentKey;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class JsonKeySerde extends Serdes.WrapperSerde<IncidentKey> {
    public JsonKeySerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(IncidentKey.class));
    }
}
