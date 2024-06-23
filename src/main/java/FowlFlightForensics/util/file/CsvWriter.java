package FowlFlightForensics.util.file;

import FowlFlightForensics.domain.dto.IncidentRanked;
import FowlFlightForensics.util.BaseComponent;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The {@code CsvWriter} class is responsible for exporting the processed data to a CSV file.
 */
public class CsvWriter extends BaseComponent {
    /**
     * Exports the provided information to a CSV file, using the provided filename.
     * @param incidents A {@code Map} containing the years as keys, as well as a {@code List} of {@code IncidentRanked}
     *                  objects in the corresponding values.
     * @param filename The filename to be used when saving the CSV file.
     */
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
