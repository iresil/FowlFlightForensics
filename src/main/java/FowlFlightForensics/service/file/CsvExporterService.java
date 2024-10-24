package FowlFlightForensics.service.file;

import FowlFlightForensics.domain.dto.IncidentGrouped;
import FowlFlightForensics.domain.dto.IncidentKey;
import FowlFlightForensics.domain.dto.IncidentRanked;
import FowlFlightForensics.service.kafka.ConsumerService;
import FowlFlightForensics.util.file.CsvWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The {@code CsvExporterService} periodically triggers the export to file process and applies some transformations, to
 * ensure that both output CSV files are generated with the same structure.
 */
@Service
public class CsvExporterService {
    @Value("${app.result.incidents.per-year.limit}")
    private int topNIncidentsPerYearLimit;

    private final CsvWriter csvWriter = new CsvWriter();

    /**
     * Periodically reads the {@code Map}s stored by the {@code Consumer}, applies transformations where necessary, and
     * stores their contents in the output CSV files.
     */
    @Scheduled(fixedRateString = "${app.consumer.write-to-file.fixed-rate}")
    public void writeTopNPerYearToCsvFiles() {
        Map<Integer, List<IncidentRanked>> result = mapToIncidentRanked(ConsumerService.incidentsGrouped);
        csvWriter.writeToCsv(result, "output_java.csv");

        csvWriter.writeToCsv(ConsumerService.incidentsRanked, "output_kafka.csv");
    }

    private Map<Integer, List<IncidentRanked>> mapToIncidentRanked(Map<IncidentKey, Long> input) {
        // Group the input data per year and species
        Map<IncidentGrouped, Long> grouped = input.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> new IncidentGrouped(entry.getKey().year(), entry.getKey().speciesId(), entry.getKey().speciesName()),
                        Collectors.summingLong(Map.Entry::getValue))
                );

        // Create a list of incidents per year
        Map<Integer, List<IncidentRanked>> aggregated = grouped.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().year(),
                        Collectors.mapping(entry -> new IncidentRanked(0, entry.getKey().year(),
                                        entry.getKey().speciesId(), entry.getKey().speciesName(), entry.getValue()),
                                Collectors.toList())
                ));

        // Sort the map by year, ascending
        List<Map.Entry<Integer, List<IncidentRanked>>> sorted = aggregated.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList();

        // Sort the value of each year's entry by count of incidents, descending, and only keep the top N items
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
