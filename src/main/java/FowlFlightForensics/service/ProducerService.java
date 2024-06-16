package FowlFlightForensics.service;

import FowlFlightForensics.FowlFlightForensicsApplication;
import FowlFlightForensics.domain.IncidentContainer;
import FowlFlightForensics.domain.IncidentSummary;
import FowlFlightForensics.util.BaseComponent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
public class ProducerService extends BaseComponent {
    @Value("${app.kafka.topics.raw}")
    private String rawDataTopic;

    private static final Integer NUM_OF_MESSAGES = 1000;

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final IncidentContainer incidentContainer = IncidentContainer.INSTANCE.getInstance();
    private List<IncidentSummary> incidentSummaryList;

    @PostConstruct
    public void init() {
        incidentSummaryList = incidentContainer.getIncidentSummaryList();
    }

    @Scheduled(cron = "0/1 * * * * ?")
    public void produceMessages() {
        Iterator<IncidentSummary> iterator = incidentSummaryList.iterator();
        List<IncidentSummary> itemsToSend = new ArrayList<>();
        for (int i = 0; i < incidentSummaryList.size() && i < NUM_OF_MESSAGES; i++) {
            IncidentSummary incident = iterator.next();
            itemsToSend.add(incident);
            iterator.remove();
        }

        logger.info("Sending {} messages to {} [remaining: {}] ...", (NUM_OF_MESSAGES < itemsToSend.size() ? NUM_OF_MESSAGES : itemsToSend.size()),
                rawDataTopic, incidentSummaryList.size());
        LongStream.range(0, NUM_OF_MESSAGES).forEach(i -> {
            if (i < itemsToSend.size()) {
                sendMessageWithKeyRecord(rawDataTopic, itemsToSend.get((int) i).getKey(),
                        itemsToSend.get((int) i));
            }
        });

        if (itemsToSend.isEmpty() && FowlFlightForensicsApplication.lastMessageTimeInMillis == -1) {
            FowlFlightForensicsApplication.lastMessageTimeInMillis = System.currentTimeMillis();
        }
    }

    public void sendMessageWithKeyRecord(final String topic, final Object key, final Object value) {
        CompletableFuture<SendResult<Object, Object>> future = kafkaTemplate.send(generateProducerRecord(topic, key, value));

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.trace("{}:{}, delivered to {}@{}.", result.getProducerRecord().key(),
                        result.getProducerRecord().value(), result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                logger.error("Unable to deliver message {}:{}", key, value, ex);
            }
        });
    }

    private ProducerRecord<Object, Object> generateProducerRecord(final String topic, final Object key,
                                                                  final Object value) {
        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(topic, key, value);
        producerRecord.headers().add(new RecordHeader("Key", UUID.randomUUID().toString().getBytes()));

        return producerRecord;
    }
}
