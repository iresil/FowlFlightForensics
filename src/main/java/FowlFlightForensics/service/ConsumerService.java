package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentGrouped;
import FowlFlightForensics.domain.IncidentKey;
import FowlFlightForensics.domain.IncidentRanked;
import FowlFlightForensics.util.BaseComponent;
import com.opencsv.CSVWriter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ConsumerService extends BaseComponent {
    @Value("${app.result.incidents.per-year.limit}")
    private int topNIncidentsPerYearLimit;

    private Map<IncidentKey, Long> speciesCount = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${app.kafka.topics.grouped.incidents}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "1")
    public void consumeMessages(ConsumerRecord<IncidentKey, Long> consumerRecord) {
        speciesCount.put(consumerRecord.key(), consumerRecord.value());

        logger.trace("Received {}:'{}' from {}@{}@{}.", consumerRecord.key(), consumerRecord.value(),
                consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
    }

    @Scheduled(fixedRateString = "${app.consumer.write-to-file.fixed-rate}")
    public void writeTopNPerYearToCsv() {
        Map<IncidentGrouped, Long> grouped = speciesCount.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> new IncidentGrouped(entry.getKey().year(), entry.getKey().speciesId(), entry.getKey().speciesName()),
                        Collectors.summingLong(Map.Entry::getValue))
                );
        Map<Integer, List<IncidentRanked>> aggregated = grouped.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().year(),
                        Collectors.mapping(entry -> new IncidentRanked(0, entry.getKey().year(),
                                entry.getKey().speciesId(), entry.getKey().speciesName(), entry.getValue()),
                                Collectors.toList())
                ));
        List<Map.Entry<Integer, List<IncidentRanked>>> sorted = aggregated.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList();
        sorted.forEach(entry -> {
            List<IncidentRanked> innerList = entry.getValue();
            List<IncidentRanked> sortedInnerList = innerList.stream()
                    .sorted((i1, i2) -> Long.compare(i2.amount(), i1.amount()))
                    .limit(topNIncidentsPerYearLimit).toList();
            List<IncidentRanked> modifiedSortedList = sortedInnerList.stream().map(i ->
                    new IncidentRanked(sortedInnerList.indexOf(i), i.year(), i.speciesId(),
                            i.speciesName(), i.amount())).toList();
            entry.setValue(modifiedSortedList);
        });
        Map<Integer, List<IncidentRanked>> result = sorted.stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try (CSVWriter writer = new CSVWriter(new FileWriter("data/output.csv"))) {
            for (Map.Entry<Integer, List<IncidentRanked>> entry : result.entrySet()) {
                for (IncidentRanked innerEntry : entry.getValue()) {
                    writer.writeNext(new String[]{
                            innerEntry.index().toString(), innerEntry.year().toString(), innerEntry.speciesId(),
                            innerEntry.speciesName(), innerEntry.amount().toString()
                    });
                }
            }
        } catch (IOException e) {
            logger.error("Error writing results to CSV file", e);
        }
    }
}
