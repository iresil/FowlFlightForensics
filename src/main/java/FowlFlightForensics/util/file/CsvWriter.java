package FowlFlightForensics.util.file;

import FowlFlightForensics.domain.IncidentRanked;
import FowlFlightForensics.util.BaseComponent;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CsvWriter extends BaseComponent {
    public void writeToCsv(Map<Integer, List<IncidentRanked>> incidents, String filename) {
        try (CSVWriter writer = new CSVWriter(new FileWriter("data/" + filename))) {
            for (Map.Entry<Integer, List<IncidentRanked>> entry : incidents.entrySet()) {
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
