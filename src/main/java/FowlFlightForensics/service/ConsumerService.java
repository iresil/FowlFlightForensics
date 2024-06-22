package FowlFlightForensics.service;

import FowlFlightForensics.domain.dto.IncidentGrouped;
import FowlFlightForensics.domain.dto.IncidentKey;
import FowlFlightForensics.domain.dto.IncidentRanked;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.file.CsvWriter;
import FowlFlightForensics.util.serdes.JsonRankedSerde;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ConsumerService extends BaseComponent {
    @Value("${app.result.incidents.per-year.limit}")
    private int topNIncidentsPerYearLimit;

    private Deserializer<List<IncidentRanked>> deserializer = Serdes.ListSerde(ArrayList.class, new JsonRankedSerde()).deserializer();

    private final CsvWriter csvWriter = new CsvWriter();

    private Map<IncidentKey, Long> incidentsGrouped = new ConcurrentHashMap<>();
    private Map<Integer, List<IncidentRanked>> incidentsRanked = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${app.kafka.topics.grouped.incidents}", groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "1", containerFactory = "groupedListenerContainerFactory")
    public void consumeGroupedMessages(ConsumerRecord<IncidentKey, Long> consumerRecord) {
        incidentsGrouped.put(consumerRecord.key(), consumerRecord.value());

        logger.trace("Received {}:'{}' from {}@{}@{}.", consumerRecord.key(), consumerRecord.value(),
                consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
    }

    @KafkaListener(topics = "${app.kafka.topics.grouped.top-n}", groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "1", containerFactory = "rankedListenerContainerFactory")
    public void consumeRankedMessages(ConsumerRecord<Integer, byte[]> consumerRecord) {
        List<IncidentRanked> value = deserializer.deserialize("", consumerRecord.value());
        incidentsRanked.put(consumerRecord.key(), value);

        logger.trace("Received {}:'{}' from {}@{}@{}.", consumerRecord.key(), consumerRecord.value(),
                consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
    }

    @Scheduled(fixedRateString = "${app.consumer.write-to-file.fixed-rate}")
    public void writeTopNPerYearToCsvFiles() {
        Map<Integer, List<IncidentRanked>> result = mapToIncidentRanked(incidentsGrouped);
        csvWriter.writeToCsv(result, "output_java.csv");

        csvWriter.writeToCsv(incidentsRanked, "output_kafka.csv");
    }

    private Map<Integer, List<IncidentRanked>> mapToIncidentRanked(Map<IncidentKey, Long> input) {
        Map<IncidentGrouped, Long> grouped = input.entrySet().stream()
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
        return sorted.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
