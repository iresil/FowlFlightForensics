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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ConsumerService extends BaseComponent {
    private Map<IncidentKey, Long> speciesCount = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${app.kafka.topics.grouped.incidents}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "1")
    public void consumeMessages(ConsumerRecord<IncidentKey, Long> consumerRecord) {
        speciesCount.put(consumerRecord.key(), consumerRecord.value());

        logger.trace("Received {}:'{}' from {}@{}@{}.", consumerRecord.key(), consumerRecord.value(),
                consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
    }

    @Scheduled(fixedRate = 5000)
    public void writeTopNPerYearToCsv() {
        Map<Integer, Map<String, Long>> local = speciesCount.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().year(),
                        Collectors.toMap(
                                entry -> entry.getKey().speciesName(),
                                Map.Entry::getValue,
                                Long::sum
                        )
                ));
        List<Map.Entry<Integer, Map<String, Long>>> resultEntries = local.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList();
        resultEntries.forEach(entry -> {
            Map<String, Long> innerMap = entry.getValue();
            Map<String, Long> sortedInnerMap = innerMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(5)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            entry.setValue(sortedInnerMap);
        });
        Map<Integer, Map<String, Long>> result = resultEntries.stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try (CSVWriter writer = new CSVWriter(new FileWriter("data/output.csv"))) {
            for (Map.Entry<Integer, Map<String, Long>> entry : result.entrySet()) {
                for (Map.Entry<String, Long> innerEntry : entry.getValue().entrySet()) {
                    writer.writeNext(new String[]{
                            entry.getKey().toString(), innerEntry.getKey(), innerEntry.getValue().toString()});
                }
            }
        } catch (IOException e) {
            logger.error("Error writing results to CSV file", e);
        }
    }
}
