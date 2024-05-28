package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentKey;
import FowlFlightForensics.domain.IncidentSummary;
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
    @Value("${app.kafka.topics.invalid-species}")
    private String invalidSpeciesTopic;

    @Bean
    public KStream<IncidentKey, IncidentSummary> streamFilter(StreamsBuilder builder) {
        var keySerde = new JsonKeySerde();
        var incidentSerde = new JsonValueSerde();

        var invalidSpeciesStream = builder.stream(rawDataTopic, Consumed.with(keySerde, incidentSerde))
                .filter((k, v) -> k.speciesName().isEmpty());

        invalidSpeciesStream.to(invalidSpeciesTopic);
        invalidSpeciesStream.print(Printed.toSysOut());

        return invalidSpeciesStream;
    }
}
