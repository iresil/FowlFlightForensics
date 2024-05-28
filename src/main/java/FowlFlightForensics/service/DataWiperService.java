package FowlFlightForensics.service;

import FowlFlightForensics.util.BaseComponent;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class DataWiperService extends BaseComponent {
    @Autowired
    private AdminClient adminClient;

    public void delete() {
        Map<TopicPartition, RecordsToDelete> deleteMap = new HashMap<>();
        try {
            adminClient.listTopics().names().get().forEach(topic -> {
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
                        deleteMap.put(new TopicPartition(topic, partitionIndex), RecordsToDelete.beforeOffset(lowWatermark));
                    }
                }
            });
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Couldn't prepare topic deletion.", e);
        }

        DeleteRecordsResult result = adminClient.deleteRecords(deleteMap);
        result.lowWatermarks().forEach((tp, lowWatermark) ->
        {
            try {
                logger.info("Data deletion for topic {} completed. Low watermark for partition {}: {}", tp.topic(), tp.partition(), lowWatermark.get().lowWatermark());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Couldn't confirm topic data deletion.");
            }
        });
    }
}
