package FowlFlightForensics.service;

import FowlFlightForensics.domain.*;
import FowlFlightForensics.enums.InvalidIncidentTopic;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.serdes.JsonKeySerde;
import FowlFlightForensics.util.serdes.JsonValueSerde;
import FowlFlightForensics.util.serdes.JsonGroupedSerde;
import FowlFlightForensics.util.serdes.JsonRankedSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

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

    @Value("${app.result.incidents.distinct.time-window.seconds}")
    private long distinctTimeWindowSeconds;
    @Value("${app.result.incidents.per-year.limit}")
    private int topNIncidentsPerYearLimit;

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
                        Materialized.<IncidentKey, Long, KeyValueStore<Bytes, byte[]>> as("REDUCE-STATE-STORE-" + UUID.randomUUID())
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
    public KStream<IncidentGrouped, Long> getTopEntries(StreamsBuilder builder) {
        JsonKeySerde keySerde = new JsonKeySerde();
        JsonGroupedSerde groupedSerde = new JsonGroupedSerde();
        JsonRankedSerde countSerde = new JsonRankedSerde();

        // Use only the last values within a time window
        KStream<IncidentGrouped, Long> groupedIncidentStream = builder.stream(groupedIncidentsTopic, Consumed.with(keySerde, Serdes.Long()))
                .groupByKey(Grouped.with(keySerde, Serdes.Long()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(distinctTimeWindowSeconds)))
                .reduce((v1, v2) -> v2,
                        Materialized.<IncidentKey, Long, WindowStore<Bytes, byte[]>> as("WINDOW-STATE-STORE-" + UUID.randomUUID())
                                .withKeySerde(keySerde)
                                .withValueSerde(Serdes.Long())
                ).suppress(Suppressed.untilTimeLimit(Duration.ofSeconds(distinctTimeWindowSeconds), Suppressed.BufferConfig.unbounded())
                        .withName("SUPPRESS-STATE-STORE-" + UUID.randomUUID()))
                .toStream().map((key, value) -> KeyValue.pair(key.key(), value))
                .map((k, v) -> KeyValue.pair(new IncidentGrouped(k.year(), k.speciesId(), k.speciesName()), v));

        // Count incidents caused by each species within the same year
        KStream<Integer, IncidentRanked> aggregatedIncidentStream = groupedIncidentStream.groupByKey(Grouped.with(groupedSerde, Serdes.Long()))
                .aggregate(
                        // Initialize the result
                        () -> 0L,
                        // Perform the addition
                        (k, v, agg) -> agg + v,
                        Materialized.<IncidentGrouped, Long, KeyValueStore<Bytes, byte[]>> as("AGGREGATE-STATE-STORE-" + UUID.randomUUID())
                                .withKeySerde(groupedSerde)
                                .withValueSerde(Serdes.Long())
                )
                .toStream()
                .map((k, v) -> KeyValue.pair(k.year(), new IncidentRanked(0, k.year(), k.speciesId(), k.speciesName(), v)));

        // Get the top N species per year that caused aircraft accidents
        aggregatedIncidentStream.groupByKey(Grouped.with(Serdes.Integer(), countSerde))
                .aggregate(
                        // Initialize a new ArrayList
                        ArrayList::new,
                        // Invoke the aggregator for each item
                        (k, v, agg) -> {
                            // Add the new item as the last element in the list
                            agg.add(v);
                            // Sort the ArrayList by amount, largest first
                            agg.sort((i1, i2) -> Long.compare(((IncidentRanked)i2).amount(), ((IncidentRanked)i1).amount()));
                            // Only return the first N items, if available
                            int upper = Math.min(agg.size(), topNIncidentsPerYearLimit);
                            return new ArrayList<>(agg.subList(0, upper));
                        }, Materialized.<Integer, List<IncidentRanked>, KeyValueStore<Bytes, byte[]>> as("FINAL-STATE-STORE-" + UUID.randomUUID())
                                .withKeySerde(Serdes.Integer())
                                .withValueSerde(Serdes.ListSerde(ArrayList.class, countSerde))
                )
                .toStream()
                .map((k, v) -> KeyValue.pair(k, ((List<IncidentRanked>)v).stream().map(i -> new IncidentRanked(((List<IncidentRanked>)v).indexOf(i),
                        i.year(), i.speciesId(), i.speciesName(), i.amount())).collect(Collectors.toList())))
                .to(topNIncidentsTopic, Produced.with(Serdes.Integer(), Serdes.ListSerde(ArrayList.class, countSerde)));

        KStream<Integer, List<IncidentRanked>> finalStream = builder.stream(topNIncidentsTopic, Consumed.with(Serdes.Integer(),
                Serdes.ListSerde(ArrayList.class, countSerde)));
        finalStream.print(Printed.toSysOut());

        return groupedIncidentStream;
    }
}
