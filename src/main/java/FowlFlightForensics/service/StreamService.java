package FowlFlightForensics.service;

import FowlFlightForensics.domain.*;
import FowlFlightForensics.enums.InvalidIncidentTopic;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.serdes.JsonKeySerde;
import FowlFlightForensics.util.serdes.JsonResultSerde;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class StreamService extends BaseComponent {
    @Value("${app.kafka.topics.raw}")
    private String rawDataTopic;
    @Value("${app.kafka.topics.clean}")
    private String cleanDataTopic;
    @Value("${app.kafka.topics.grouped.creatures}")
    private String groupedCreaturesTopic;
    @Value("${app.kafka.topics.grouped.incidents}")
    private String groupedIncidentsTopic;
    @Value("${app.kafka.topics.grouped.top-n}")
    private String topNIncidentsTopic;

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
                .branch((k, v) -> k.aircraftDamage(), Branched.withConsumer(kstream -> kstream.to(cleanDataTopic)))
                .noDefaultBranch();

        //KStream<IncidentKey, IncidentSummary> cleanIncidentStream = builder.stream(cleanDataTopic, Consumed.with(keySerde, incidentSerde));
        //cleanIncidentStream.print(Printed.toSysOut());

        return rawIncidentStream;
    }

    @Bean
    public KStream<IncidentKey, IncidentSummary> streamGroup(StreamsBuilder builder) {
        JsonKeySerde keySerde = new JsonKeySerde();
        JsonValueSerde incidentSerde = new JsonValueSerde();

        KStream<IncidentKey, IncidentSummary> cleanIncidentStream = builder.stream(cleanDataTopic, Consumed.with(keySerde, incidentSerde));
        cleanIncidentStream.mapValues(v -> (long)((v.getSpeciesQuantityMin() + v.getSpeciesQuantityMax()) / 2))
                .groupByKey(Grouped.with(keySerde, Serdes.Long()))
                .reduce(Long::sum,
                        Materialized.<IncidentKey, Long, KeyValueStore<Bytes, byte[]>> as("AGGREGATES-STATE-STORE-" + UUID.randomUUID())
                                .withKeySerde(keySerde)
                                .withValueSerde(Serdes.Long())
                )
                .toStream()
                .to(groupedCreaturesTopic);
        cleanIncidentStream.groupByKey(Grouped.with(keySerde, incidentSerde))
                .count(Materialized.<IncidentKey, Long, KeyValueStore<Bytes, byte[]>> as("COUNT-STATE-STORE-" + UUID.randomUUID())
                                .withKeySerde(keySerde)
                                .withValueSerde(Serdes.Long())
                )
                .toStream()
                .to(groupedIncidentsTopic);

        //KStream<IncidentKey, Long> groupedIncidentStream = builder.stream(groupedIncidentsTopic, Consumed.with(keySerde, Serdes.Long()));
        //groupedIncidentStream.print(Printed.toSysOut());

        return cleanIncidentStream;
    }

    @Bean
    public KStream<IncidentResult, Long> getTopEntries(StreamsBuilder builder) {
        JsonKeySerde keySerde = new JsonKeySerde();
        JsonResultSerde resultSerde = new JsonResultSerde();

        KStream<IncidentResult, Long> groupedIncidentStream = builder.stream(groupedIncidentsTopic, Consumed.with(keySerde, Serdes.Long()))
                .map((k, v) -> KeyValue.pair(new IncidentResult(k.year(), k.speciesId(), k.speciesName()), v));
        groupedIncidentStream.groupByKey(Grouped.with(resultSerde, Serdes.Long()))
                .aggregate(
                        // Initialize the result
                        () -> 0L,
                        // Invoke the aggregator for each item
                        (k, v, agg) -> agg + v,
                        Materialized.<IncidentResult, Long, KeyValueStore<Bytes, byte[]>> as("FINAL-STATE-STORE-" + UUID.randomUUID())
                                .withKeySerde(resultSerde)
                                .withValueSerde(Serdes.Long())
                )
                .toStream()
                .to(topNIncidentsTopic, Produced.with(resultSerde, Serdes.Long()));

        KStream<IncidentResult, Long> finalStream = builder.stream(topNIncidentsTopic, Consumed.with(resultSerde, Serdes.Long()));
        finalStream.print(Printed.toSysOut());

        return groupedIncidentStream;
    }
}
