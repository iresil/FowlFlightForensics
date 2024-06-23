package FowlFlightForensics.service.kafka;

import FowlFlightForensics.domain.dto.IncidentKey;
import FowlFlightForensics.domain.dto.IncidentRanked;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.serdes.JsonRankedSerde;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code ConsumerService} receives data from two topics and stores that data in the corresponding {@code Map}.
 */
@Service
public class ConsumerService extends BaseComponent {
    private Deserializer<List<IncidentRanked>> deserializer = Serdes.ListSerde(ArrayList.class, new JsonRankedSerde()).deserializer();

    public static Map<IncidentKey, Long> incidentsGrouped = new ConcurrentHashMap<>();
    public static Map<Integer, List<IncidentRanked>> incidentsRanked = new ConcurrentHashMap<>();

    /**
     * Consumes messages from the grouped incidents topic and stores those messages in {@code incidentsGrouped}.
     * @param consumerRecord The received message.
     */
    @KafkaListener(topics = "${app.kafka.topics.grouped.incidents}", groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "1", containerFactory = "groupedListenerContainerFactory")
    public void consumeGroupedMessages(ConsumerRecord<IncidentKey, Long> consumerRecord) {
        incidentsGrouped.put(consumerRecord.key(), consumerRecord.value());

        logger.trace("Received {}:'{}' from {}@{}@{}.", consumerRecord.key(), consumerRecord.value(),
                consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
    }

    /**
     * Consumes messages from the top N topic, deserializes them accordingly, and stores them in {@code incidentsRanked}.
     * @param consumerRecord The received message.
     */
    @KafkaListener(topics = "${app.kafka.topics.grouped.top-n}", groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "1", containerFactory = "rankedListenerContainerFactory")
    public void consumeRankedMessages(ConsumerRecord<Integer, byte[]> consumerRecord) {
        List<IncidentRanked> value = deserializer.deserialize("", consumerRecord.value());
        incidentsRanked.put(consumerRecord.key(), value);

        logger.trace("Received {}:'{}' from {}@{}@{}.", consumerRecord.key(), consumerRecord.value(),
                consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
    }
}
