package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentContainer;
import FowlFlightForensics.domain.IncidentKey;
import FowlFlightForensics.domain.IncidentSummary;
import FowlFlightForensics.enums.InvalidIncidentTopic;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.serdes.JsonKeySerde;
import FowlFlightForensics.util.serdes.JsonValueSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class StreamService extends BaseComponent {
    @Value("${app.kafka.topics.raw}")
    private String rawDataTopic;
    @Value("${app.kafka.topics.cleaned}")
    private String cleanedDataTopic;
    @Value("${app.kafka.topics.grouped.creatures}")
    private String groupedCreaturesTopic;
    @Value("${app.kafka.topics.grouped.incidents}")
    private String groupedIncidentsTopic;

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
                    kstream.map((k, v) -> new KeyValue<>(k, (long)((v.getSpeciesQuantityMin() + v.getSpeciesQuantityMax()) / 2)))
                            .filter((k, v) -> k.aircraftDamage())
                            .groupBy((k, v) -> k, Grouped.with(keySerde, Serdes.Long()))
                            .reduce(Long::sum,
                                    Materialized.<IncidentKey, Long, KeyValueStore<Bytes, byte[]>> as("AGGREGATES-STATE-STORE-" + UUID.randomUUID())
                                            .withKeySerde(keySerde)
                                            .withValueSerde(Serdes.Long())
                                            //.withStoreType(Materialized.StoreType.IN_MEMORY)
                                            //.withRetention(Duration.ofSeconds(1L))
                                            //.withCachingDisabled()
                            )
                            .toStream()
                            .to(groupedCreaturesTopic);
                    kstream.filter((k, v) -> k.aircraftDamage())
                            .groupBy((k, v) -> k, Grouped.with(keySerde, incidentSerde))
                            .count(Materialized.<IncidentKey, Long, KeyValueStore<Bytes, byte[]>> as("COUNT-STATE-STORE-" + UUID.randomUUID())
                                    .withKeySerde(keySerde)
                                    .withValueSerde(Serdes.Long())
                                    //.withStoreType(Materialized.StoreType.IN_MEMORY)
                                    //.withRetention(Duration.ofSeconds(1L))
                                    //.withCachingDisabled())
                            )
                            .toStream()
                            .to(groupedIncidentsTopic);
                }));

        KStream<IncidentKey, Long> groupedIncidentStream = builder.stream(groupedIncidentsTopic, Consumed.with(keySerde, Serdes.Long()));
        groupedIncidentStream.print(Printed.toSysOut());

        return rawIncidentStream;
    }
}
