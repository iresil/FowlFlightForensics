package FowlFlightForensics.util.fileHandling;

import FowlFlightForensics.domain.IncidentDetails;
import FowlFlightForensics.util.BaseComponent;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Component
public class CsvReader extends BaseComponent {
    public List<List<String>> zippedCsvToListOfStringValues() {
        List<List<String>> records = new ArrayList<>();
        try (GZIPInputStream gZIPInputStream = new GZIPInputStream(new FileInputStream("data/dataset.gz"));) {
            BufferedReader br = new BufferedReader(new InputStreamReader(gZIPInputStream));
            CSVReader csvReader = new CSVReader(br);

            String[] values = null;
            while ((values = csvReader.readNext()) != null) {
                records.add(Arrays.asList(values));
            }
        } catch (CsvValidationException e) {
            logger.error("Invalid CSV file", e);
        } catch (IOException e) {
            logger.error("Unable to open CSV file for reading", e);
        }
        return records;
    }

    public List<IncidentDetails> zippedCsvToListOfObjects() {
        List<IncidentDetails> records = new ArrayList<>();
        try (GZIPInputStream gZIPInputStream = new GZIPInputStream(new FileInputStream("data/dataset.gz"));) {
            BufferedReader br = new BufferedReader(new InputStreamReader(gZIPInputStream));

            CsvToBean<IncidentDetails> cb = new CsvToBeanBuilder<IncidentDetails>(br)
                    .withType(IncidentDetails.class)
                    .build();
            records = cb.parse();
        } catch (IOException e) {
            logger.error("Unable to open CSV file for reading", e);
        }
        return records;
    }
}