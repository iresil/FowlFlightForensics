package FowlFlightForensics.util.serdes;

import FowlFlightForensics.domain.IncidentResult;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class JsonResultSerde extends Serdes.WrapperSerde<IncidentResult> {
    public JsonResultSerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(IncidentResult.class));
    }
}
