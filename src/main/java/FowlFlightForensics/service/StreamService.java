package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentContainer;
import FowlFlightForensics.domain.IncidentKey;
import FowlFlightForensics.domain.IncidentSummary;
import FowlFlightForensics.enums.InvalidIncidentTopic;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.serdes.JsonKeySerde;
import FowlFlightForensics.util.serdes.JsonValueSerde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamService extends BaseComponent {
    @Value("${app.kafka.topics.raw}")
    private String rawDataTopic;

    private final IncidentContainer incidentContainer = IncidentContainer.INSTANCE.getInstance();

    @Bean
    public KStream<IncidentKey, IncidentSummary> streamFilter(StreamsBuilder builder) {
        JsonKeySerde keySerde = new JsonKeySerde();
        JsonValueSerde incidentSerde = new JsonValueSerde();

        KStream<IncidentKey, IncidentSummary> invalidSpeciesStream = builder.stream(rawDataTopic, Consumed.with(keySerde, incidentSerde))
                .filter((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.SPECIES));
        invalidSpeciesStream.to(InvalidIncidentTopic.SPECIES.getAnnotationValue());
        //invalidSpeciesStream.print(Printed.toSysOut());

        KStream<IncidentKey, IncidentSummary> invalidQuantityStream = builder.stream(rawDataTopic, Consumed.with(keySerde, incidentSerde))
                .filter((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.QUANTITY));
        invalidQuantityStream.to(InvalidIncidentTopic.QUANTITY.getAnnotationValue());
        //invalidQuantityStream.print(Printed.toSysOut());

        KStream<IncidentKey, IncidentSummary> invalidGenericStream = builder.stream(rawDataTopic, Consumed.with(keySerde, incidentSerde))
                .filter((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.OTHER));
        invalidGenericStream.to(InvalidIncidentTopic.OTHER.getAnnotationValue());
        //invalidGenericStream.print(Printed.toSysOut());

        return invalidSpeciesStream;
    }
}
