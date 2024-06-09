package FowlFlightForensics.util.serdes;

import FowlFlightForensics.domain.TopNContainer;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Deprecated
public class JsonResultSerde extends Serdes.WrapperSerde<TopNContainer> {
    public JsonResultSerde() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(TopNContainer.class));
    }
}
