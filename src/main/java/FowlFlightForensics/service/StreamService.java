package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentContainer;
import FowlFlightForensics.domain.IncidentKey;
import FowlFlightForensics.domain.IncidentSummary;
import FowlFlightForensics.enums.InvalidIncidentTopic;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.serdes.JsonKeySerde;
import FowlFlightForensics.util.serdes.JsonValueSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamService extends BaseComponent {
    @Value("${app.kafka.topics.raw}")
    private String rawDataTopic;
    @Value("${app.kafka.topics.grouped}")
    private String groupedDataTopic;

    private final IncidentContainer incidentContainer = IncidentContainer.INSTANCE.getInstance();

    @Bean
    public KStream<IncidentKey, IncidentSummary> streamFilter(StreamsBuilder builder) {
        JsonKeySerde keySerde = new JsonKeySerde();
        JsonValueSerde incidentSerde = new JsonValueSerde();

        KStream<IncidentKey, IncidentSummary> rawIncidentStream = builder.stream(rawDataTopic, Consumed.with(keySerde, incidentSerde));
        rawIncidentStream.split()
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).size() == 3,
                        Branched.withConsumer(kstream -> {
                            kstream.to(InvalidIncidentTopic.SPECIES.getAnnotationValue());
                            kstream.to(InvalidIncidentTopic.QUANTITY.getAnnotationValue());
                            kstream.to(InvalidIncidentTopic.OTHER.getAnnotationValue());
                        }))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.SPECIES)
                        && incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.QUANTITY),
                        Branched.withConsumer(kstream -> {
                            kstream.to(InvalidIncidentTopic.SPECIES.getAnnotationValue());
                            kstream.to(InvalidIncidentTopic.QUANTITY.getAnnotationValue());
                        }))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.SPECIES)
                                && incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.OTHER),
                        Branched.withConsumer(kstream -> {
                            kstream.to(InvalidIncidentTopic.SPECIES.getAnnotationValue());
                            kstream.to(InvalidIncidentTopic.OTHER.getAnnotationValue());
                        }))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.QUANTITY)
                                && incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.OTHER),
                        Branched.withConsumer(kstream -> {
                            kstream.to(InvalidIncidentTopic.QUANTITY.getAnnotationValue());
                            kstream.to(InvalidIncidentTopic.OTHER.getAnnotationValue());
                        }))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.SPECIES),
                        Branched.withConsumer(kstream -> kstream.to(InvalidIncidentTopic.SPECIES.getAnnotationValue())))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.QUANTITY),
                        Branched.withConsumer(kstream -> kstream.to(InvalidIncidentTopic.QUANTITY.getAnnotationValue())))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.OTHER),
                        Branched.withConsumer(kstream -> kstream.to(InvalidIncidentTopic.OTHER.getAnnotationValue())))
                .defaultBranch(Branched.withConsumer(kstream -> {
                    kstream.groupBy((k, v) -> k, Grouped.with(keySerde, incidentSerde))
                            .count()
                            .toStream()
                            .to(groupedDataTopic);
                }));

        KStream<IncidentKey, Long> groupedIncidentStream = builder.stream(groupedDataTopic, Consumed.with(keySerde, Serdes.Long()))
                .filter((k, v) -> k.aircraftDamage());
        groupedIncidentStream.print(Printed.toSysOut());

        return rawIncidentStream;
    }
}
