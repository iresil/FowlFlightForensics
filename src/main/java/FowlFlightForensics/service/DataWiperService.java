package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentKey;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.serdes.JsonKeySerde;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class DataWiperService extends BaseComponent {
    @Autowired
    private AdminClient adminClient;

    @Value("${app.kafka.topics.grouped.incidents}")
    private String groupedDataTopic;

    @Autowired
    StreamsBuilder builder;

    public void delete() {
        deleteLocalStateStore();

        Map<TopicPartition, RecordsToDelete> partitionsForCleanup = getPartitionsForCleanup();
        deleteMessages(partitionsForCleanup);
    }

    private void deleteLocalStateStore() {
        try {
            FileSystemUtils.deleteRecursively(Paths.get(Paths.get(".").normalize().toAbsolutePath().toString(),
                    "kafka-streams"));
            logger.info("Recursive cleanup of kafka streams directory successfully completed.");
        } catch (IOException e) {
            logger.error("Couldn't clean kafka streams directory.");
        }
    }

    private Map<TopicPartition, RecordsToDelete> getPartitionsForCleanup() {
        Map<TopicPartition, RecordsToDelete> result = new HashMap<>();
        try {
            adminClient.listTopics().names().get().forEach(topic -> {
                if (!topic.contains("STATE-STORE")) {
                    int partitionCount = 0;
                    try {
                        partitionCount = adminClient.describeTopics(Collections.singleton(topic)).topicNameValues().get(topic).get().partitions().size();
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("Couldn't retrieve partition count for topic {}", topic, e);
                    }

                    for (int partitionIndex = 0; partitionIndex < partitionCount; partitionIndex++) {
                        TopicPartition partition = new TopicPartition(topic, partitionIndex);
                        OffsetSpec latestOffsetSpec = OffsetSpec.latest();

                        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsets = new HashMap<>();
                        try {
                            offsets = adminClient.listOffsets(Map.of(partition, latestOffsetSpec)).all().get();
                        } catch (InterruptedException | ExecutionException e) {
                            logger.error("Couldn't retrieve offsets for topic {}, partition {}", topic, partition.toString(), e);
                        }

                        for (Map.Entry<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> entry : offsets.entrySet()) {
                            long lowWatermark = offsets.get(entry.getKey()).offset();
                            result.put(new TopicPartition(topic, partitionIndex), RecordsToDelete.beforeOffset(lowWatermark));
                        }
                    }
                }
            });
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Couldn't prepare topic deletion.", e);
        }
        return result;
    }

    private void deleteMessages(Map<TopicPartition, RecordsToDelete> partitionsForCleanup) {
        DeleteRecordsResult result = adminClient.deleteRecords(partitionsForCleanup);
        result.lowWatermarks().forEach((tp, lowWatermark) ->
        {
            try {
                logger.info("Data deletion for topic {} completed. Low watermark for partition {}: {}", tp.topic(), tp.partition(), lowWatermark.get().lowWatermark());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Couldn't confirm topic data deletion.", e);
            }
        });
    }

    @Deprecated
    private void streamCleanup() {
        JsonKeySerde keySerde = new JsonKeySerde();

        KStream<IncidentKey, Long> rawIncidentStream = builder.stream(groupedDataTopic, Consumed.with(keySerde, Serdes.Long()));
        rawIncidentStream.map((k, v) -> new KeyValue<>(k, 0L))
                .groupBy((k, v) -> k, Grouped.with(keySerde, Serdes.Long()))
                .reduce(Long::sum,
                        Materialized.<IncidentKey, Long, KeyValueStore<Bytes, byte[]>> as("AGGREGATES-STATE-STORE")
                                .withKeySerde(keySerde)
                                .withValueSerde(Serdes.Long())
                                .withStoreType(Materialized.StoreType.IN_MEMORY)
                                //.withRetention(Duration.ofSeconds(1L))
                                //.withCachingDisabled()
                        )
                .toStream()
                .to(groupedDataTopic);
    }
}
