package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentKey;
import FowlFlightForensics.util.BaseComponent;
import com.opencsv.CSVWriter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ConsumerService extends BaseComponent {
    private Map<IncidentKey, Long> incidentsCount = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${app.kafka.topics.grouped.incidents}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "1")
    public void consumeMessages(ConsumerRecord<IncidentKey, Long> consumerRecord) {
        incidentsCount.put(consumerRecord.key(), consumerRecord.value());

        logger.trace("Received {}:'{}' from {}@{}@{}.", consumerRecord.key(), consumerRecord.value(),
                consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
    }

    @Scheduled(fixedRate = 5000)
    public void writeTop10MessagesToCsv() {
        Map<IncidentKey, Long> result = incidentsCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        try (CSVWriter writer = new CSVWriter(new FileWriter("data/output.csv"))) {
            for (Map.Entry<IncidentKey, Long> entry : result.entrySet()) {
                writer.writeNext(new String[] {
                        entry.getKey().year().toString(), entry.getKey().month().toString(),
                        entry.getKey().speciesName(), entry.getValue().toString() });
            }
        } catch (IOException e) {
            logger.error("Error writing results to CSV file", e);
        }
    }
}
